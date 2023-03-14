package com.tgbots.prodavan.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Ffmpeg {
    public static void mergeVA(String inputVideo,String inputAudio,String merged){
        List<String> commands = new ArrayList<>();
        commands.add("ffmpeg");
        commands.add("-y");
        commands.add("-i");
        commands.add(inputVideo);
        commands.add("-i");
        commands.add(inputAudio);
        commands.add("-c");
        commands.add("copy");
        commands.add(merged);

        ProcessBuilder processBuilder = new ProcessBuilder(commands);
        Process process = null;
        try {
            process = processBuilder.start();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String s = bufferedReader.readLine();
            while (s!=null){
                System.out.println(s);
                s = bufferedReader.readLine();
            }

            process.waitFor();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }



}
