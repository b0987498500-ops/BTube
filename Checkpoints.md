# BTube 專案 Checkpoint 歷史異動日誌 (Checkpoints.md)

本檔案由 **Checkpoint Master** 自動維護，記錄專案每個 SemVer 快照點 (vX.XX) 的異動與規則狀態。

---

## 📌 [v1.01] - 2026-08-16 10:54 (修復雲端打包 Build Workflow)

### 🌟 核心功能與變更摘要
1. **修復 GitHub Actions 雲端建置**：
   - 新增 `gradle.properties`（啟用 AndroidX 與記憶體最佳化）。
   - 在 `.github/workflows/build_apk.yml` 中新增 `gradle wrapper` 雲端自動補全步驟，修復缺少 `gradle-wrapper.jar` 導致的 10 秒建置失敗問題。

---

## 📌 [v1.00] - 2026-08-16 10:44 (初始發布/穩定版)

### 🌟 核心功能與變更摘要
1. **Pocket Mode (黑幕防誤觸模式)**：
   - 採用純黑 `#000000` 遮罩覆蓋全螢幕。
   - 自動將螢幕亮度降至最低 (`0.01f`)，適應 OLED 螢幕省電。
   - 綁定 `FLAG_KEEP_SCREEN_ON` 防止播放中被系統關屏。
   - 全盤攔截觸控事件 (`onTouchListener` 傳回 `true`) 防止口袋摩擦誤觸。
   - 實作高精度連點解鎖機制：**500ms 內快速連續點擊螢幕 8 次** 即可解除黑幕。
2. **近接感應器 (Proximity Sensor)**：
   - 監聽 `Sensor.TYPE_PROXIMITY`。
   - 遮擋（放入口袋 / 螢幕朝下放置）時自動開啟黑幕模式。
   - 移開遮擋（從口袋拿出）時自動恢復正常介面。
3. **睡眠定時器 (Sleep Timer)**：
   - 預設 30 分鐘倒數，支援介面手動 `-10分`、`+10分`、`重置30分`。
   - 計時時間到時自動執行 `document.querySelector('video')?.pause()` 暫停播放。
   - 自動釋放 `FLAG_KEEP_SCREEN_ON` 讓手機恢復硬體自動休眠關屏。
4. **YouTube WebView 整合**：
   - 載入 `https://m.youtube.com`，啟用 JavaScript、DOM Storage、CookieManager 跨站 Cookie，支援 Google 帳號直接登入、搜尋、查看播放歷史與歌單。
5. **GitHub Actions 雲端 APK 自動打包工作流**：
   - 設定 `.github/workflows/build_apk.yml`，推送代碼至 GitHub 時自動免費雲端編譯出 Debug APK 供手機下載安裝。

---

### 📦 專案連結
- **GitHub 倉庫**: `https://github.com/b0987498500-ops/BTube.git`
- **當前版本 Tag**: `v1.01`
