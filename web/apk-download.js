const downloadButton = document.getElementById("apk-download");

if (downloadButton) {
  downloadButton.addEventListener("click", async (event) => {
    event.preventDefault();
    if (downloadButton.dataset.busy === "true") return;
    downloadButton.dataset.busy = "true";
    const originalLabel = downloadButton.textContent;
    const parts = [];
    try {
      for (let index = 0; index < 16; index++) {
        downloadButton.textContent = `Preparing Milo… ${index + 1}/16`;
        const name = `Milo-Android-Test.apk.part${String(index).padStart(2, "0")}`;
        const response = await fetch(name, { credentials: "same-origin" });
        if (!response.ok) throw new Error(`Part ${index + 1} could not be downloaded`);
        parts.push(await response.arrayBuffer());
      }
      const byteLength = parts.reduce((total, part) => total + part.byteLength, 0);
      if (byteLength !== 63700118) throw new Error("Download size did not match");
      const apk = new Blob(parts, { type: "application/vnd.android.package-archive" });
      const digest = await crypto.subtle.digest("SHA-256", await apk.arrayBuffer());
      const hash = Array.from(new Uint8Array(digest), (byte) => byte.toString(16).padStart(2, "0")).join("").toUpperCase();
      if (hash !== "9AE1EB40906E2C06DBDF9AC57B7B60B81FFB70F6CBF7092F234C127E410BFBCE") throw new Error("Download integrity check failed");
      const objectUrl = URL.createObjectURL(apk);
      const link = document.createElement("a");
      link.href = objectUrl;
      link.download = "Milo-Android-Test.apk";
      document.body.append(link);
      link.click();
      link.remove();
      setTimeout(() => URL.revokeObjectURL(objectUrl), 60000);
      downloadButton.textContent = "Download started · tap again if needed";
    } catch (error) {
      downloadButton.textContent = `Download failed. Tap to retry.`;
    } finally {
      downloadButton.dataset.busy = "false";
      if (downloadButton.textContent === originalLabel) downloadButton.textContent = "Download Android APK";
    }
  });
}
