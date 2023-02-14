package com.tgbots.prodavan.service;

import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.downloader.YoutubeCallback;
import com.github.kiulian.downloader.downloader.YoutubeProgressCallback;
import com.github.kiulian.downloader.downloader.request.RequestVideoFileDownload;
import com.github.kiulian.downloader.downloader.request.RequestVideoInfo;
import com.github.kiulian.downloader.downloader.response.Response;
import com.github.kiulian.downloader.model.videos.VideoDetails;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.model.videos.formats.Format;
import com.github.kiulian.downloader.model.videos.formats.VideoWithAudioFormat;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Service
public class YoutubeService {
    YoutubeDownloader downloader = new YoutubeDownloader();
    List<String> getVideoQuality(String videoId){
        List<String> quality = new ArrayList<>();
        RequestVideoInfo request = new RequestVideoInfo(videoId)
                .callback(new YoutubeCallback<VideoInfo>() {
                    @Override
                    public void onFinished(VideoInfo videoInfo) {
                        System.out.println("Finished parsing");
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        System.out.println("Error: " + throwable.getMessage());
                    }
                })
                .async();
        Response<VideoInfo> response = downloader.getVideoInfo(request);
        VideoInfo video = response.data(); // will block thread
        // get videos formats only with audio
        List<VideoWithAudioFormat> videoWithAudioFormats = video.videoWithAudioFormats();
        videoWithAudioFormats.forEach(it -> {
            quality.add(it.videoQuality().toString());
        });
      return quality;
    };
    void downloadVideo(String videoId,String videoQuality){
        RequestVideoInfo request = new RequestVideoInfo(videoId);
        Response<VideoInfo> response = downloader.getVideoInfo(request);
        VideoInfo video = response.data();
        List<VideoWithAudioFormat> videoWithAudioFormats = video.videoWithAudioFormats();

        File outputDir = new File("my_videos");
        Format format = videoWithAudioFormats.stream().filter(it->it.videoQuality().toString().equals(videoQuality)).findFirst().orElse(null);

        RequestVideoFileDownload videorequest = new RequestVideoFileDownload(format)
                .saveTo(outputDir) // by default "videos" directory
                .renameTo(videoId+videoQuality)
                .overwriteIfExists(true)
                .callback(new YoutubeProgressCallback<File>() {
                    @Override
                    public void onDownloading(int progress) {
                        System.out.printf("Downloaded %d%%\n", progress);
                    }

                    @Override
                    public void onFinished(File videoInfo) {
                        System.out.println("Finished file: " + videoInfo);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        System.out.println("Error: " + throwable.getLocalizedMessage());
                    }
                })
                .async();
        Response<File> videoResponse = downloader.downloadVideoFile(videorequest);
        File data = videoResponse.data();
    }

    public static void main(String[] args) {
//        YoutubeDownloader downloader = new YoutubeDownloader();
//        String videoId = "4K5Fm-1ZYTY";
//        RequestVideoInfo request = new RequestVideoInfo(videoId)
//                .callback(new YoutubeCallback<VideoInfo>() {
//                    @Override
//                    public void onFinished(VideoInfo videoInfo) {
//                        System.out.println("Finished parsing");
//                    }
//
//                    @Override
//                    public void onError(Throwable throwable) {
//                        System.out.println("Error: " + throwable.getMessage());
//                    }
//                })
//                .async();
//        Response<VideoInfo> response = downloader.getVideoInfo(request);
//        VideoInfo video = response.data(); // will block thread
//        // get videos formats only with audio
//        List<VideoWithAudioFormat> videoWithAudioFormats = video.videoWithAudioFormats();
//        videoWithAudioFormats.forEach(it -> {
//            System.out.println("video+audio: "+it.audioQuality() + ", " + it.videoQuality() + " : " + it.url());
//        });
//        Format format = videoWithAudioFormats.get(videoWithAudioFormats.toArray().length-1);
////        System.out.println("----------------------------");
////        List<VideoFormat> videoFormats = video.videoFormats();
////        videoFormats.forEach(it -> {
////            System.out.println("without audio "+it.qualityLabel() + " : " + it.url());
////        });
//        File outputDir = new File("my_videos");
////        Format format = videoWithAudioFormats.stream().filter(it->it.qualityLabel().equals("1080p")).findFirst().orElse(null);
////        System.out.println("_______________________________");
////        System.out.println(format.extension());
//        RequestVideoFileDownload request1 = new RequestVideoFileDownload(format)
//                .callback(new YoutubeProgressCallback<File>() {
//                    @Override
//                    public void onDownloading(int progress) {
//                        System.out.printf("Downloaded %d%%\n", progress);
//                    }
//
//                    @Override
//                    public void onFinished(File videoInfo) {
//                        System.out.println("Finished file: " + videoInfo);
//                    }
//
//                    @Override
//                    public void onError(Throwable throwable) {
//                        System.out.println("Error: " + throwable.getLocalizedMessage());
//                    }
//                })
//                .async();
//
//        Response<File> response1 = downloader.downloadVideoFile(request1);
//        File data = response1.data();
//        System.out.println(data);

    }
}
