import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Tone {

    public static void main(String[] args) throws Exception {
        List<BellNote> song = null;

        song = loadSong(args[0]);

        final AudioFormat af =
            new AudioFormat(Note.SAMPLE_RATE, 8, 1, true, false);
        Tone t = new Tone(af);
        t.playSong(song);
    }

    private final AudioFormat af;

    Tone(AudioFormat af) {
        this.af = af;
    }

    void playSong(List<BellNote> song) throws LineUnavailableException {
        try (final SourceDataLine line = AudioSystem.getSourceDataLine(af)) {
            line.open();
            line.start();

            for (BellNote bn: song) {
                playNote(line, bn);
            }
            line.drain();
        }
    }

    private static List<BellNote> loadSong(String filename) {
        List<BellNote> list = new ArrayList<BellNote>();
        String line = null;

        try (FileReader fr = new FileReader(filename);
             BufferedReader br = new BufferedReader(fr);) {
            while((line = br.readLine()) != null) {
                BellNote bn = new BellNote();
                String[] split = line.split(" ");

                bn.note = parseNote(split[0]);
                bn.length = parseNoteLength(split[1]);

                list.add(bn);
            }
        } catch (IOException ignored) {}

        return list;
    }

    private static Note parseNote(String note) {
        if (note == null) {
            return Note.INVALID;
        }

        switch (note.toUpperCase().trim()) {
            case "A4":
                return Note.A4;
            case "A4S":
                return Note.A4S;
            case "B4":
                return Note.B4;
            case "C4":
                return Note.C4;
            case "C4S":
                return Note.C4S;
            case "D4":
                return Note.D4;
            case "D4S":
                return Note.D4S;
            case "E4":
                return Note.E4;
            case "F4":
                return Note.F4;
            case "F4S":
                return Note.F4S;
            case "G4":
                return Note.G4;
            case "G4S":
                return Note.G4S;
            case "A5":
                return Note.A5;
        }
        return Note.INVALID;
    }

    private static NoteLength parseNoteLength(String nl) {
        if (nl == null) {
            return NoteLength.INVALID;
        }

        switch (nl) {
            case "1":
                return NoteLength.WHOLE;
            case "2":
                return NoteLength.HALF;
            case "4":
                return NoteLength.QUARTER;
            case "8":
                return NoteLength.EIGHTH;
        }
        return NoteLength.INVALID;
    }

    private void playNote(SourceDataLine line, BellNote bn) {
        final int ms = Math.min(bn.length.timeMs(), Note.MEASURE_LENGTH_SEC * 1000);
        final int length = Note.SAMPLE_RATE * ms / 1000;
        line.write(bn.note.sample(), 0, length);
        line.write(Note.REST.sample(), 0, 50);
    }
}

class BellNote {
    Note note;
    NoteLength length;
}

enum NoteLength {
    INVALID(0.0f),
    WHOLE(1.0f),
    HALF(0.5f),
    QUARTER(0.25f),
    EIGHTH(0.125f);

    private final int timeMs;

    private NoteLength(float length) {
        timeMs = (int)(length * Note.MEASURE_LENGTH_SEC * 1000);
    }

    public int timeMs() {
        return timeMs;
    }
}

enum Note {
    // REST Must be the first 'Note'
    REST,
    INVALID,
    A4,
    A4S,
    B4,
    C4,
    C4S,
    D4,
    D4S,
    E4,
    F4,
    F4S,
    G4,
    G4S,
    A5;

    public static final int SAMPLE_RATE = 48 * 1024; // ~48KHz
    public static final int MEASURE_LENGTH_SEC = 1;

    // Circumference of a circle divided by # of samples
    private static final double step_alpha = (2.0d * Math.PI) / SAMPLE_RATE;

    private final double FREQUENCY_A_HZ = 440.0d;
    private final double MAX_VOLUME = 127.0d;

    private final byte[] sinSample = new byte[MEASURE_LENGTH_SEC * SAMPLE_RATE];

    private Note() {
        int n = this.ordinal();
        if (n > 0) {
            // Calculate the frequency!
            final double halfStepUpFromA = n - 1;
            final double exp = halfStepUpFromA / 12.0d;
            final double freq = FREQUENCY_A_HZ * Math.pow(2.0d, exp);

            // Create sinusoidal data sample for the desired frequency
            final double sinStep = freq * step_alpha;
            for (int i = 0; i < sinSample.length; i++) {
                sinSample[i] = (byte)(Math.sin(i * sinStep) * MAX_VOLUME);
            }
        }
    }

    public byte[] sample() {
        return sinSample;
    }
}
