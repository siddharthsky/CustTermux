<h1 align="center">
  <br>
  <a href="https://github.com/siddharthsky/ai-video-summarizer-and-timestamp-generator-LLM-p"><img src="https://github.com/siddharthsky/CustTermux-JioTVGo/blob/5e4151627498699e8f2b030479a4abc69f638a85/_assets/full-pg-cir-crop.png" alt="AI YouTube Video Summarizer" width="200"></a>
  <br>
   📺 CustTermux-JioTVGo
  <br>
</h1>

<h4 align="center">Fork of the Termux to run server automatically. </h4>



### Usage

1. Install an IPTV player (currently supported for autostart):
    1. OTT Navigator
    2. Televiso
    3. Sparkle TV

2. Install the CustTermux APK from the [release page](https://github.com/siddharthsky/CustTermux-JioTVGo/releases).

3. Open CustTermux.

4. It will automatically download the required files and ask you to select a default IPTV player. You can select "none" to just run the server. You can always use [SparkleTV2](https://github.com/siddharthsky/SparkleTV2-auto-service) to select any other IPTV player.

5. In the end, it will redirect you to the login page. URL - [http://localhost:5001](http://localhost:5001)

6. Login via OTP.

7. Restart your device -or- force stop CustTermux.

8. Now, when you open CustTermux, it will start the server and automatically open the selected IPTV player.

9. In IPTV player, Add playlist url as : http://localhost:5001/playlist.m3u

### Note

1. Need to update Go binary/ Need to change IPTV player/ Termux force stopping / Having problems
    - Clear App Data and Rerun.
  
2. If you are running server on Phone and using on TV.
   - In initial setup, Select "none" when asked to choose iptv player.
   - Add this url in IPTV player[TV] http://{phone ip address}:5001/playlist.m3u 
    


