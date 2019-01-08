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
  private double bpm;
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

    try {
      this.length = Long.parseLong(info.get("length"));
    } catch (NumberFormatException e) {
      this.length = song.getLengthInMilliseconds();
    }

    try {
      this.bpm = Double.parseDouble((info.get("bpm")));
    } catch (NullPointerException | NumberFormatException f) {
      this.bpm = 0.0;
    }

    if (info.get("key") != null) {
      this.key = new Key(info.get("key"));
    }

    getMetadata();

    int startBeatIntro;
    int introBeatLength;
    int startBeatOurto;
    int outroBeatLength;
    long firstBeat;

    try {
      startBeatIntro = Integer.parseInt(info.get("startBeatIntro"));
      introBeatLength = Integer.parseInt(info.get("introBeatLength"));
    } catch (NumberFormatException e) {
      startBeatIntro = 0;
      introBeatLength = 0;
    }

    try {
      startBeatOurto = Integer.parseInt(info.get("startBeatOutro"));
      outroBeatLength = Integer.parseInt(info.get("outroBeatLength"));
    } catch (NumberFormatException e) {
      startBeatOurto = 0;
      outroBeatLength = 0;
    }

    try {
      firstBeat = Long.parseLong(info.get("firstBeat"));
    } catch (NumberFormatException e) {
      firstBeat = 0;
    }

    this.beatGrid = new BeatGrid(this.length, this.bpm, firstBeat, startBeatIntro, introBeatLength,
        startBeatOurto, outroBeatLength);

  }

}