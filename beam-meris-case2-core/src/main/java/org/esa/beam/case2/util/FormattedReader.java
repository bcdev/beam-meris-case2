package org.esa.beam.case2.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.StringTokenizer;

/**
 * Handles formatted input in a FORTRAN like manner:
 * if only a part of a line is read, the rest of the line is lost.<p>
 * There might be comment-lines, which will be skipped.<p>
 * Data items can be separated by delimiters, which could change within the file.
 *
 * @author H. Schiller / GKSS
 */
public class FormattedReader {

    private RandomAccessFile inp;
    private String delimiters;
    private boolean fileHasComments;
    private boolean echoComments;
    private String commentBegin;

    /**
     * this <code>org.esa.beam.lakes.spain.util.FormattedReader</code> will have:
     * <pre>
     * this.delimiters=" \t\n\r,;:";
     * this.file_has_comments=true;
     * this.echo_comments=false;
     * this.comment_begin="#";
     * </pre>
     */
    public FormattedReader(RandomAccessFile inp) {
        this.inp = inp;
        this.delimiters = " \t\n\r,;:";
        this.fileHasComments = true;
        this.echoComments = false;
        this.commentBegin = "#";
    }

    /**
     * this <code>org.esa.beam.lakes.spain.util.FormattedReader</code> will have:
     * <pre>
     * this.file_has_comments=true;
     * this.echo_comments=false;
     * this.comment_begin="#";
     * </pre>
     */
    public FormattedReader(RandomAccessFile inp, String delimiters) {
        this.inp = inp;
        this.delimiters = delimiters;
        this.fileHasComments = true;
        this.echoComments = false;
        this.commentBegin = "#";
    }

    /**
     * this <code>org.esa.beam.lakes.spain.util.FormattedReader</code> will have:
     * <pre>
     * this.delimiters=" \t\n\r,;:";
     * this.file_has_comments=true;
     * this.echo_comments=false;
     * this.comment_begin="#";
     * </pre>
     */
    public FormattedReader(String filename) throws FileNotFoundException {
        this.inp = new RandomAccessFile(filename, "r");
        this.delimiters = " \t\n\r,;:";
        this.fileHasComments = true;
        this.echoComments = false;
        this.commentBegin = "#";
    }

    /**
     * this <code>org.esa.beam.lakes.spain.util.FormattedReader</code> will have:
     * <pre>
     * this.file_has_comments=true;
     * this.echo_comments=false;
     * this.comment_begin="#";
     * </pre>
     */
    public FormattedReader(String filename, String delimiters) throws FileNotFoundException {
        this.inp = new RandomAccessFile(filename, "r");
        this.delimiters = delimiters;
        this.fileHasComments = true;
        this.echoComments = false;
        this.commentBegin = "#";
    }


    /**
     * Method commentStart Method for setting the comment begin String.
     *
     * @param comment_begin The String associated with a comment.
     */
    public void commentStart(String comment_begin) {
        this.commentBegin = comment_begin;
        this.fileHasComments = true;
    }

    /**
     * Method noComments
     */
    public void noComments() {
        this.fileHasComments = false;
    }

    /**
     * Method setEcho
     *
     * @param echo_comments
     */
    public void setEcho(boolean echo_comments) {
        this.echoComments = echo_comments;
    }

    /**
     * Method setDelimiters
     *
     * @param delimiters
     */
    public void setDelimiters(String delimiters) {
        this.delimiters = delimiters;
    }

    /**
     * Method rlong Read just one long.
     *
     * @return The long just read.
     */
    public long rlong() throws IOException {
        String read;
        boolean ready = false;
        long res = 0;
        try {
            while (!ready) {
                read = inp.readLine();
                if (this.fileHasComments) {
                    if (read.startsWith(this.commentBegin)) {
                        if (this.echoComments) {
                            System.out.println(read);
                        }
                        continue;
                    }
                }
                StringTokenizer st = new StringTokenizer(read, this.delimiters);
                res = Long.parseLong(st.nextToken());
                ready = true;
            }
        } catch (NumberFormatException e) {
            throw new IOException("number conversion error");
        }
        return res;
    }

    /**
     * Method rdouble Read just one double.
     *
     * @return The double just read.
     */
    public double rdouble() throws IOException {
        String eing;
        boolean ready = false;
        double res = 0;
        try {
            while (!ready) {
                eing = inp.readLine();
                if (eing == null) {
                    break;
                }
                if (this.fileHasComments) {
                    if (eing.startsWith(this.commentBegin)) {
                        if (this.echoComments) {
                            System.out.println(eing);
                        }
                        continue;
                    }
                }
                StringTokenizer st = new StringTokenizer(eing, this.delimiters);
                res = Double.valueOf(st.nextToken());
                ready = true;
            }
        } catch (NumberFormatException e) {
            throw new IOException("number conversion error");
        }
        return res;
    }

    /**
     * Method rString Read just one line (skipping comments, delimiters dont care)
     *
     * @return The line just read.
     */
    public String rString() throws IOException {
        String eing = null;
        boolean ready = false;
        while (!ready) {
            eing = inp.readLine();
            if (eing == null) {
                break;
            }
            if (this.fileHasComments) {
                if (eing.startsWith(this.commentBegin)) {
                    if (this.echoComments) {
                        System.out.println(eing);
                    }
                    continue;
                }
            }
            ready = true;
        }
        return eing;
    }

    /**
     * Method rlong Read some long's.
     *
     * @param how_many long's should be read
     *
     * @return less than <code>how_many</code> long's if EOF is met
     */
    public long[] rlong(int how_many) throws IOException {
        String eing;
        boolean ready = false;
        long[] res = new long[how_many];
        int got = 0;
        try {
            while (!ready) {
                eing = inp.readLine();
                if (eing == null) {
                    break;
                }
                if (this.fileHasComments) {
                    if (eing.startsWith(this.commentBegin)) {
                        if (this.echoComments) {
                            System.out.println(eing);
                        }
                        continue;
                    }
                }
                StringTokenizer st =
                        new StringTokenizer(eing, this.delimiters);
                int nn = st.countTokens();
                for (int i = 0; i < nn; i++) {
                    res[got] = Long.parseLong(st.nextToken());
                    got++;
                    if (got == how_many) {
                        ready = true;
                        break;
                    }
                }
            }
        } catch (NumberFormatException e) {
            throw new IOException("number conversion error");
        }
        if (got == how_many) {
            return res;
        } else {
            long[] less = new long[got];
            System.arraycopy(res, 0, less, 0, got);
            return less;
        }
    }

    /**
     * Method rdouble Read some double's.
     *
     * @param how_many double's should be read
     *
     * @return less than <code>how_many</code> double's if EOF is met.
     */
    public double[] rdouble(int how_many) throws IOException {
        String eing;
        boolean ready = false;
        double[] res = new double[how_many];
        int got = 0;
        try {
            while (!ready) {
                eing = inp.readLine();
                if (eing == null) {
                    break;
                }
                if (this.fileHasComments) {
                    if (eing.startsWith(this.commentBegin)) {
                        if (this.echoComments) {
                            System.out.println(eing);
                        }
                        continue;
                    }
                }
                StringTokenizer st =
                        new StringTokenizer(eing, this.delimiters);
                int nn = st.countTokens();
                for (int i = 0; i < nn; i++) {
                    res[got] = Double.valueOf(st.nextToken());
                    got++;
                    if (got == how_many) {
                        ready = true;
                        break;
                    }
                }
            }
        } catch (NumberFormatException e) {
            throw new IOException("number conversion error");
        }
        if (got == how_many) {
            return res;
        } else {
            double[] less = new double[got];
            System.arraycopy(res, 0, less, 0, got);
            return less;
        }
    }

    /**
     * Method rdoubleAll Read array of points of known dimension.
     *
     * @param dimension components belong to one point
     *
     * @return the points[][dimension] read until EOF is met
     */
    public double[][] rdoubleAll(int dimension) throws IOException {
        double[][] points;
        int npoints = 0;
        long merke = this.inp.getFilePointer();
        double[] p;
        while (true) {
            p = this.rdouble(dimension);
            if (p.length < dimension) {
                break;
            }
            npoints++;
        }
        this.inp.seek(merke);
        points = new double[npoints][];
        for (int i = 0; i < npoints; i++) {
            points[i] = this.rdouble(dimension);
        }
        return points;
    }

    /**
     * Method rdoubleAll Read array of points.
     * The dimension of the points is judged from the actual input line.
     *
     * @return The points[][] read until EOF is met.
     */
    public double[][] rdoubleAll() throws IOException {
        long merke = this.inp.getFilePointer();
        String eing = this.rString();
        this.inp.seek(merke);
        StringTokenizer st = new StringTokenizer(eing, this.delimiters);
        return this.rdoubleAll(st.countTokens());
    }

    /**
     * Closes this reader and releases any system
     * resources associated with the underlying stream. A closed reader
     * cannot perform input or output operations and cannot be
     * reopened.
     *
     * @throws IOException if an I/O error occurs.
     */
    public void close() throws IOException {
        this.inp.close();
    }
}
