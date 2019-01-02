package contextproject.models;

import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;

import contextproject.helpers.StackTrace;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Hashtable;

public class Track {
  private static Logger log = LogManager.getLogger(Track.class.getName());

  private Mp3File song;
  private String title;
  private String artist;
  private String album;
  private String absolutePath;
  private long length;
  private float bpm;
  private Key key;
  private BeatGrid beatGrid;

  /**
   * constructor of a track.
   * 
   * @param abPath
   *          Path of the mp3 file
   */
  public Track(String abPath, Hashtable<String, String> info) {

    // String title, String artist, String album, long length, double bpm, String key, long
    // firstBeat, int beatIntroStart, int beatsIntroLength, int beatOutroStart, int beatsOutroLength
    try {
      song = new Mp3File(abPath);
    } catch (UnsupportedTagException e) {
      log.error("There was a Unsupported tag exception with file:" + abPath);
      log.trace(StackTrace.stackTrace(e));
    } catch (InvalidDataException e) {
      log.error("There was a Invalid data exception with file:" + abPath);
      log.trace(StackTrace.stackTrace(e));
    } catch (IOException e) {
      log.error("There was a IO exception with file:" + abPath);
      log.trace(StackTrace.stackTrace(e));
    }
    this.absolutePath = abPath;
    this.title = info.get("title");
    this.artist = info.get("artist");
    this.album = info.get("album");
    this.length = Long.parseLong(info.get("length"));
    this.bpm = Float.parseFloat(info.get("bpm"));
    if (info.get("key") != null) {
      this.key = new Key(info.get("key"));
    }
    getMetadata();
    this.beatGrid = new BeatGrid(this.length, this.bpm, Long.parseLong(info.get("firstBeat")),
        Integer.parseInt(info.get("startBeatIntro")),
        Integer.parseInt(info.get("introBeatLength")),
        Integer.parseInt(info.get("startBeatOutro")), 
        Integer.parseInt(info.get("outroBeatLength")));
  }
}