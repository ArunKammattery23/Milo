import argparse
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch, MagicMock
import wave
import voice_lab


class VoiceLabTests(unittest.TestCase):
    def test_silent_wav_is_reported_without_log_error(self):
        with tempfile.TemporaryDirectory() as d:
            path = Path(d) / "silence.wav"
            with wave.open(str(path), "wb") as w:
                w.setparams((1, 2, 24000, 0, "NONE", "not compressed"))
                w.writeframes(b"\0\0" * 24000)
            info = voice_lab.inspect_wav(path)
            self.assertEqual(info["seconds"], 1)
            self.assertIsNone(info["rms_dbfs"])

    def test_empty_text_rejected(self):
        with self.assertRaises(ValueError):
            voice_lab.eleven_payload("  ", "neutral")

    def test_hosted_request_sends_text_and_saves_audio(self):
        with tempfile.TemporaryDirectory() as d:
            output = Path(d) / "out.mp3"
            response = MagicMock()
            response.headers = {"Content-Type": "audio/mpeg"}
            response.read.return_value = b"ID3-test-audio"
            context = MagicMock()
            context.__enter__.return_value = response
            args = argparse.Namespace(text="Hello", emotion="happy", preview=False, output=output)
            with patch.dict(os.environ, {"ELEVENLABS_API_KEY": "test-key", "ELEVENLABS_VOICE_ID": "test-voice"}), patch("urllib.request.urlopen", return_value=context) as call:
                voice_lab.eleven(args)
            request = call.call_args.args[0]
            self.assertEqual(json.loads(request.data)["text"], "[happy] Hello")
            self.assertEqual(json.loads(request.data)["model_id"], "eleven_v3")
            self.assertEqual(output.read_bytes(), b"ID3-test-audio")

    def test_preview_never_calls_network(self):
        args = argparse.Namespace(text="Hello", emotion="neutral", preview=True)
        with patch("urllib.request.urlopen") as call, patch("builtins.print"):
            voice_lab.eleven(args)
            call.assert_not_called()


if __name__ == "__main__":
    unittest.main()
