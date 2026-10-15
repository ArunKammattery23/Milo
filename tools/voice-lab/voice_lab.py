"""Milo voice audition tool. No network calls unless a synthesis command is chosen."""
import argparse
import array
import json
import math
import os
from pathlib import Path
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import wave

TAGS = {"neutral": "", "happy": "[happy] ", "comfort": "[reassuring] ",
        "excited": "[excited] ", "sleepy": "[sleepy] "}


def inspect_wav(path):
    with wave.open(str(path), "rb") as w:
        if w.getsampwidth() != 2 or w.getcomptype() != "NONE":
            raise ValueError("Inspection requires uncompressed 16-bit PCM WAV.")
        samples = array.array("h", w.readframes(w.getnframes()))
        if sys.byteorder != "little":
            samples.byteswap()
        if not samples:
            raise ValueError("The recording is empty.")
        peak = max(abs(s) for s in samples)
        rms = math.sqrt(sum(s*s for s in samples) / len(samples))
        return {"seconds": w.getnframes() / w.getframerate(),
                "sample_rate": w.getframerate(), "channels": w.getnchannels(),
                "peak_dbfs": round(20*math.log10(peak/32768), 2) if peak else None,
                "rms_dbfs": round(20*math.log10(rms/32768), 2) if rms else None,
                "full_scale_samples": sum(s >= 32767 or s <= -32768 for s in samples)}


def eleven_payload(text, emotion):
    if not text.strip():
        raise ValueError("Text must not be empty.")
    if len(text) > 4500:
        raise ValueError("Use a short audition under 4,500 characters.")
    return {"text": TAGS[emotion] + text, "model_id": "eleven_v3"}


def eleven(args):
    payload = eleven_payload(args.text, args.emotion)
    if args.preview:
        print(json.dumps(payload, ensure_ascii=False, indent=2))
        return
    key = os.environ.get("ELEVENLABS_API_KEY", "")
    voice = os.environ.get("ELEVENLABS_VOICE_ID", "")
    if not key or not voice:
        raise ValueError("Set ELEVENLABS_API_KEY and ELEVENLABS_VOICE_ID locally first.")
    url = "https://api.elevenlabs.io/v1/text-to-speech/" + urllib.parse.quote(voice, safe="")
    req = urllib.request.Request(url + "?output_format=mp3_44100_128",
          data=json.dumps(payload).encode("utf-8"),
          headers={"xi-api-key": key, "Content-Type": "application/json", "Accept": "audio/mpeg"})
    try:
        with urllib.request.urlopen(req, timeout=90) as response:
            if "audio/" not in response.headers.get("Content-Type", ""):
                raise ValueError("Provider returned a non-audio response; no file written.")
            data = response.read(20_000_001)
    except urllib.error.HTTPError as e:
        raise ValueError(f"Voice API returned HTTP {e.code}. Check key, voice access and quota; no automatic retry.") from None
    if not data or len(data) > 20_000_000:
        raise ValueError("Provider returned empty or oversized audio.")
    args.output.write_bytes(data)


def local(args):
    if not args.text.strip():
        raise ValueError("Text must not be empty.")
    inspect_wav(args.reference)
    # Import only on demand; inspect and hosted auditions need no extra packages.
    import torchaudio
    from chatterbox.tts import ChatterboxTTS
    model = ChatterboxTTS.from_pretrained(device=args.device)
    audio = model.generate(args.text, audio_prompt_path=str(args.reference),
                           exaggeration=args.exaggeration, cfg_weight=0.3)
    torchaudio.save(str(args.output), audio, model.sr)


def main():
    p = argparse.ArgumentParser(description=__doc__)
    sub = p.add_subparsers(dest="command", required=True)
    inspect = sub.add_parser("inspect", help="Inspect a WAV locally; never uploads it")
    inspect.add_argument("reference", type=Path)
    cloud = sub.add_parser("eleven", help="Paid hosted audition, or offline request preview")
    cloud.add_argument("--text", required=True)
    cloud.add_argument("--emotion", choices=TAGS, default="neutral")
    cloud.add_argument("--preview", action="store_true")
    cloud.add_argument("--output", type=Path, default=Path("audition.mp3"))
    clone = sub.add_parser("local", help="English Chatterbox reference-voice audition")
    clone.add_argument("--reference", type=Path, required=True)
    clone.add_argument("--text", required=True)
    clone.add_argument("--device", choices=["cpu", "cuda"], default="cpu")
    clone.add_argument("--exaggeration", type=float, choices=[0.3, 0.5, 0.7], default=0.5)
    clone.add_argument("--output", type=Path, default=Path("audition.wav"))
    args = p.parse_args()
    try:
        if args.command == "inspect":
            print(json.dumps(inspect_wav(args.reference), indent=2))
            return
        preview = getattr(args, "preview", False)
        if not preview:
            if args.output.exists():
                raise ValueError("Output already exists. Choose another filename.")
            expected = ".mp3" if args.command == "eleven" else ".wav"
            if args.output.suffix.lower() != expected:
                raise ValueError("Output must end in " + expected)
            args.output.parent.mkdir(parents=True, exist_ok=True)
        start = time.monotonic()
        (eleven if args.command == "eleven" else local)(args)
        if not preview:
            print(f"Saved {args.output.resolve()} in {time.monotonic()-start:.1f}s (includes loading/downloads).")
    except (ValueError, OSError, wave.Error, ImportError) as e:
        p.exit(1, f"{e}\n")


if __name__ == "__main__":
    main()
