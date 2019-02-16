package com.company;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//import com.mpatric.mp3agic.ID3v1;
//import com.mpatric.mp3agic.ID3v1Tag;
//import com.mpatric.mp3agic.ID3v24Tag;
//import com.mpatric.mp3agic.InvalidDataException;
//import com.mpatric.mp3agic.NotSupportedException;




public class Main {

    public static void main(String[] args) throws Exception {
	// write your code here

        // Step 1: Check Program Input

        if(args.length != 1) {
            throw new IllegalArgumentException("You need to specify a valid mp3 directory.");
        }

        String directory = args[0];
        Path mp3Directory = Paths.get(directory);

        if (!Files.exists(mp3Directory)) {
            throw new IllegalArgumentException("The specified directory does not exist : " + mp3Directory);
        } else {
            System.out.println("The specified directory has been found.");
        }

        // Step 2: Files

        List<Path> mp3Paths = new ArrayList<>();

        try (DirectoryStream<Path> paths = Files.newDirectoryStream(mp3Directory, "*.mp3")) {
            paths.forEach(p -> {
                System.out.println("Found : " + p.getFileName().toString());
                mp3Paths.add(p);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Step 3: Files -> Domain Classes - Part 1

        List<Song> songs = mp3Paths.stream().map(path -> {
            try {
                Mp3File mp3file = new Mp3File(path);
                ID3v2 id3 = mp3file.getId3v2Tag();
                return new Song(id3.getArtist(), id3.getYear(), id3.getAlbum(), id3.getTitle());
            } catch (IOException | UnsupportedTagException | com.mpatric.mp3agic.InvalidDataException e) {
                throw new IllegalStateException(e);
            }
        }).collect(Collectors.toList());

        System.out.println("Songs = " + songs);

        // Step 4: Domain Classes -> SQL/Database
        //         Saving the metadata to the database: H2

        try (Connection conn = DriverManager.getConnection("jdbc:h2:~/mydatabase;AUTO_SERVER=TRUE;INIT=runscript from 'classpath:create.sql'")) {
            PreparedStatement st = conn.prepareStatement("insert into SONGS (artist, year, album, title) values (?, ?, ?, ?);");

            for (Song song : songs) {
                st.setString(1, song.getArtist());
                st.setString(2, song.getYear());
                st.setString(3, song.getAlbum());
                st.setString(4, song.getTitle());
                st.addBatch();
            }

//            songs.forEach(s -> {
//                try {
//                    st.setString(1, s.getArtist());
//                    st.setString(2, s.getYear());
//                    st.setString(3, s.getAlbum());
//                    st.setString(4, s.getTitle());
//                    st.addBatch();
//                } catch (SQLException e) {
//                    e.printStackTrace();
//                }
//            });

            int[] updates = st.executeBatch();
            System.out.println("Inserted [=" + updates.length + "] records into the database");
        }

        // Step 5: Booting up a webserver
        //         Start HTTP Server - Part 1

        Server server = new Server(8080);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.setResourceBase(System.getProperty("java.io.tmpdir"));
        server.setHandler(context);

        context.addServlet(SongServlet.class, "/songs");
        server.start();

        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(new URI("http://localhost:8080/songs"));
        }


    }





    // Step 5: Write a servlet - Part 2

     public static class SongServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            StringBuilder builder = new StringBuilder();

            try (Connection conn = DriverManager.getConnection("jdbc:h2:~/mydatabase")) {

                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery("select * from SONGS");

                while (rs.next()) {
                    builder.append("<tr class=\"table\">")
                            .append("<td>").append(rs.getString("year")).append("</td>")
                            .append("<td>").append(rs.getString("artist")).append("</td>")
                            .append("<td>").append(rs.getString("album")).append("</td>")
                            .append("<td>").append(rs.getString("title")).append("</td>")
                            .append("</tr>");
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }

            String string = "<html><h1>Your Songs</h1><table><tr><th>Year</th><th>Artist</th><th>Album</th><th>Title</th></tr>" + builder.toString() + "</table></html>";
            resp.getWriter().write(string);
        }
    }

}





