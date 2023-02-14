package com.tgbots.prodavan.service;

import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.downloader.YoutubeCallback;
import com.github.kiulian.downloader.downloader.YoutubeProgressCallback;
import com.github.kiulian.downloader.downloader.request.RequestVideoFileDownload;
import com.github.kiulian.downloader.downloader.request.RequestVideoInfo;
import com.github.kiulian.downloader.downloader.response.Response;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.model.videos.formats.AudioFormat;
import com.github.kiulian.downloader.model.videos.formats.Format;
import com.github.kiulian.downloader.model.videos.formats.VideoFormat;
import com.github.kiulian.downloader.model.videos.formats.VideoWithAudioFormat;
import com.tgbots.prodavan.config.BotConfig;
import com.tgbots.prodavan.model.User;
import com.tgbots.prodavan.repositories.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
public class TelegramBot  extends TelegramLongPollingBot {

    @Autowired
    private UserRepository userRepository;
    private YoutubeDownloader downloader = new YoutubeDownloader();
    final BotConfig config;
    public TelegramBot(BotConfig config){
        this.config = config;
        List<BotCommand> listofCommands = new ArrayList<>();
        listofCommands.add(new BotCommand("/start","get welcome"));
        listofCommands.add(new BotCommand("/mydata","get your data"));
        listofCommands.add(new BotCommand("/help","get info about bot"));
        try {
            this.execute(new SetMyCommands(listofCommands,new BotCommandScopeDefault(),null));
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getBotToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if(update.hasMessage()&& update.getMessage().hasText()){
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            String pattern = "^((?:https?:)?\\/\\/)?((?:www|m)\\.)?((?:youtube(-nocookie)?\\.com|youtu.be))(\\/(?:[\\w\\-]+\\?v=|embed\\/|v\\/)?)([\\w\\-]+)(\\S+)?$";
            boolean isYoutubeLink = messageText.matches(pattern);
            if(isYoutubeLink){
                sendVideoQuality(messageText,chatId);
            }else {
                switch (messageText){
                    case "/start":
                        startCommandReceived(chatId,update.getMessage().getChat().getUserName());
                }
            }
        }else if(update.hasCallbackQuery()){
            String data = update.getCallbackQuery().getData();
            System.out.println(data);
            downloadVideo(data.split("&")[0],data.split("&")[1],update.getCallbackQuery().getMessage().getChatId());
        }
    }
    private void startCommandReceived(long chatId, String name){
        String answer = "Hi,"+ name+" nice to meet you!";

        sendMessage(chatId,answer);
    }
    private void registerUser(Message msg){
        if(userRepository.findById(msg.getChatId()).isEmpty()){
            Long chatId = msg.getChatId();
            Chat chat = msg.getChat();

            User user = User.builder()
                    .chatId(chatId)
                    .firstName(chat.getFirstName())
                    .lastName(chat.getLastName())
                    .userName(chat.getUserName())
                    .build();


            userRepository.save(user);
        }
    }

    private void sendVideoQuality(String messageText,Long chatId){
        SendMessage message = new SendMessage();
        message.setChatId(chatId);

        String videoId = messageText.split("=")[1];
        RequestVideoInfo request = new RequestVideoInfo(videoId)
                .callback(new YoutubeCallback<VideoInfo>() {
                    @Override
                    public void onFinished(VideoInfo videoInfo) {
                        System.out.println("Finished parsing");
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        message.setText("Oops, something went wrong...");
                        try {
                            execute(message);
                        } catch (TelegramApiException e) {
                            throw new RuntimeException(e);
                        }
                        System.out.println("Error: " + throwable.getMessage());
                    }
                })
                .async();
        Response<VideoInfo> response = downloader.getVideoInfo(request);
        VideoInfo video = response.data();
        if(video==null){
            sendMessage(chatId,"Oops,something went wrong...");
        }else{
            List<VideoWithAudioFormat> videoWithAudioFormats = video.videoWithAudioFormats();
            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
            List<InlineKeyboardButton> keyboardButtonsRow = new ArrayList<>();

            for (VideoWithAudioFormat it: videoWithAudioFormats) {
                InlineKeyboardButton oneButton = new InlineKeyboardButton();
                oneButton.setText(it.videoQuality().toString());
                oneButton.setCallbackData(videoId+"&"+it.videoQuality().toString());
                keyboardButtonsRow.add(oneButton);
            }
            List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
            rowList.add(keyboardButtonsRow);
            inlineKeyboardMarkup.setKeyboard(rowList);

            message.setReplyMarkup(inlineKeyboardMarkup);
            message.setText("Select quality");
            try {
                execute(message);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }

    }
    private  void downloadVideo(String videoId,String videoQuality,Long chatId){
        RequestVideoInfo request = new RequestVideoInfo(videoId);
        Response<VideoInfo> response = downloader.getVideoInfo(request);
        VideoInfo video = response.data();
        List<VideoWithAudioFormat> videoWithAudioFormats = video.videoWithAudioFormats();
        List<VideoFormat> videoFormats = video.videoFormats();
        for (VideoFormat videoFormat:videoFormats) {
            System.out.println(videoFormat.videoQuality()+" "+videoFormat.url());
        }
        System.out.println("==============================");
        List<AudioFormat> audioFormats = video.audioFormats();
        for (AudioFormat audioFormat:audioFormats) {
            System.out.println(audioFormat.audioSampleRate().toString()+" " + audioFormat.averageBitrate()+ " "+audioFormat.audioQuality()+" "+audioFormat.url());
        }

        File outputDir = new File("my_videos");
        Format format = videoWithAudioFormats.stream().filter(it->it.videoQuality().toString().equals(videoQuality)).findFirst().orElse(null);

        if(format == null){

        }
//        RequestVideoFileDownload videorequest = new RequestVideoFileDownload(format)
//                .saveTo(outputDir) // by default "videos" directory
//                .renameTo(videoId+videoQuality)
//                .overwriteIfExists(true)
//                .callback(new YoutubeProgressCallback<File>() {
//                    @Override
//                    public void onDownloading(int progress) {
//                        System.out.printf("Downloaded %d%%\n", progress);
//                    }
//
//                    @Override
//                    public void onFinished(File videoInfo) {
//
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
//        Response<File> videoResponse = downloader.downloadVideoFile(videorequest);
//        File data = videoResponse.data();
//        InputFile inputFile = new InputFile(data);
//        SendDocument sendDocument = new SendDocument();
//        sendDocument.setDocument(inputFile);
//        sendDocument.setChatId(chatId);
//        SendVideo sendVideo = new SendVideo();
//        sendVideo.setChatId(chatId);
//        sendVideo.setVideo(inputFile);
//        try {
//            execute(sendDocument);
//        } catch (TelegramApiException e) {
//            throw new RuntimeException(e);
//        }
    }
    private void sendMessage(long chatId,String textToSend){
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);
        try {
            execute(message);
        }catch (TelegramApiException e) {

        }
    }
}
