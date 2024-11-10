<h1 align="center">
  <br>
  <a href="#">
    <img src="_assets/logos/CustTermux-icon.png" alt="CustTermux Icon" width="200">
  </a>
  <br>
  📺 CustTermux
  <br>
</h1>

<p align="center">A customized version of Termux for TV streaming.</p>

<p align="center">
  <img src="https://img.shields.io/badge/version-4.8.3-blue" alt="Version">
  <img src="https://img.shields.io/badge/build-stable-brightgreen" alt="Build Status">
  <img src="https://img.shields.io/badge/license-GPLv3-orange" alt="License">
</p>

---

- [Features](#-features)
- [Installation](#-installation)
- [Usage](#-usage)
- [Login](#-login)
- [Acknowledgements](#-acknowledgements)

---

### ✨ Features
**Designed for TV Streaming:**
- **CustTermux Environment**: Tailored for a seamless TV streaming experience.
- **Built-in Media Dependencies**: Pre-configured packages for effortless setup.
- **jiotv_go Integration**: Enhanced streaming capabilities with direct support for [jiotv_go](https://github.com/rabilrbl/jiotv_go).
- **TV-Optimized UI**: Simple and accessible interface for navigation on TVs.

**Initial Setup and Configuration:**
- **Initial Setup Page**: Added for streamlined configuration.
- **Dependency Removal**: Eliminated reliance on other Termux apps.

**Enhanced Functionality:**
- **Android 5 Support**: Now included.
- **Onscreen Keys**: Ctrl, Alt, and Arrow keys are now accessible via the remote.
- **Ctrl + C Functionality**: Available through the remote, removing the need for an additional device to stop the service.
- **OnResume Functionality**: IPTV automatically opens when switching to CustTermux.

**Main Activity Updates:**
- **Button Layout**: Updated on the main page.
- **TV Layout**: Introduced for a more intuitive interface.
- **IPTV Button**: Long press to change IPTV settings; otherwise, it opens IPTV directly.
- **Settings Page**: Added for easier access to configuration options.
- **Playlist Download Option**: Download playlists based on local or public access.
- **Playlist Link**: Displayed on the home screen with a copy option.

![Screenshot](https://i.imgur.com/yGZ6JBZl.png)

**Native Layouts and Boot Features:**
- **Native Layouts**: Termux-API is no longer required.
- **Autostart on Device Boot**: Implemented without the need for Termux-boot.

**Additional Features:**
- **Live Server Status and Login Status**: Displayed for real-time updates.
- **IPTV Selector**: Choose any app for IPTV.
- **Web Player**: Added for standalone operation.
- **New Login Page**: Supports OTP and password authentication.
- **Login Checker**: Added to verify login status.

**Settings Page Enhancements:**
- **Local and Public Access Settings**: Configurable through the new Settings page.
- **Autostart Server**: Option to enable server autostart.
- **Auto-Start IPTV**: Option for automatic IPTV start.
- **Port Changing**: Option to change server port.
- **EPG Generation Settings**: Configurable EPG settings.
- **Generic TV Banner**: Option to enable/disable.
- **SSH Access Settings**: Added for secure access.

<table>
  <tr>
    <td><img src="https://i.imgur.com/0BUJ4nY.png" alt="Screenshot 1" width="400"/></td>
    <td><img src="https://i.imgur.com/lGeQXz9.png" alt="Screenshot 2" width="400"/></td>
    <td><img src="https://i.imgur.com/VUEl2yw.png" alt="Screenshot 3" width="400"/></td>
  </tr>
</table>


---

### 📥 Installation

1. **Download the APK**: Grab the latest release from the [releases page](https://github.com/siddharthsky/CustTermux/releases).
   - **Important for Android 5 & 6 Users**: If you are using Android 5 or 6, download the APK labeled specifically for older devices to ensure compatibility and optimal performance.

2. **Install the APK**:
   - Enable installation from unknown sources in your TV's settings.
   - Use a file manager to locate and install the downloaded APK.

3. **Grant Permissions**: Ensure CustTermux has the necessary permissions for storage and network access to function properly. For best performance, confirm all permissions during the initial setup.


---

### 🚀 Usage

1. **Launching CustTermux**: Open the app from your TV’s apps menu.
2. **Setting Up for the First Time**:
   - Upon the first launch, CustTermux will initialize the required environment and download dependencies.
   - Follow any on-screen instructions to configure network settings or script options.
   - Login by following the steps outlined in the **Login** section.
3. **Accessing TV Channels**:
   - Navigate to the WEB TV menu to explore available channels.
   - Select a channel to start streaming instantly!
4. **Using an IPTV Player**:
   - To play channels via an IPTV player, add the IPTV playlist URL in your preferred IPTV player app. `[http://localhost:<port>/playlist.m3u]` if set locally.
   - Open the IPTV player and navigate to the section for adding playlists.
   - Input the URL of the IPTV playlist and save it.
   - Select the playlist to browse and play available channels.

---

### 🔐 Login

To access , login in required:

1. **Open the Login Menu**: In the app settings, select *Login*.
2. **Choose Login Method**:
   - **Login via Password**:
     - Enter your phone number and password in the designated fields.
     - Click **Login** to authenticate.
   - **Login via OTP**:
     - Enter your phone number in the *Phone Number* field.
     - Click **Send OTP** to receive a one-time password on your phone.
     - Enter the received OTP in the *OTP* field, then click **Verify** to complete login.
3. **Handling Login Errors**:
   - If you attempt to log in multiple times in quick succession, the server may freeze, and a cooldown period for OTP is applied. Avoid spamming requests. If you encounter this issue, wait a few minutes before trying again.
   - Ensure you have a stable internet connection for OTP delivery and verification.

---

### 🙌 Acknowledgements

- **[Termux](https://github.com/termux)**  
  This project is based on Termux, providing a robust way to manage dependencies, scripts, and environment setup for seamless media streaming.

- **[rabilrbl](https://github.com/rabilrbl)**  
  CustTermux leverages [jiotv_go](https://github.com/rabilrbl/jiotv_go) as part of its core implementation to enhance TV streaming capabilities.
