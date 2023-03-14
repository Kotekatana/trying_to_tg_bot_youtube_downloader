package com.tgbots.prodavan.service;

import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.downloader.YoutubeProgressCallback;
import com.github.kiulian.downloader.downloader.request.RequestVideoFileDownload;
import com.github.kiulian.downloader.downloader.request.RequestVideoInfo;
import com.github.kiulian.downloader.downloader.response.Response;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.model.videos.formats.AudioFormat;
import com.github.kiulian.downloader.model.videos.formats.VideoFormat;
import com.github.kiulian.downloader.model.videos.formats.VideoWithAudioFormat;
import com.tgbots.prodavan.config.BotConfig;
import com.tgbots.prodavan.model.User;
import com.tgbots.prodavan.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
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
import java.util.*;
import java.util.stream.Collectors;

@Component
public class TelegramBot  extends TelegramLongPollingBot {
    private Ffmpeg ffmpeg = new Ffmpeg();

    @Autowired
    private UserRepository userRepository;
    private YoutubeDownloader downloader = new YoutubeDownloader();
    private YoutubeService youtubeService = new YoutubeService();
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
                        break;
                    case "/test":
                        break;
                }
            }
        }else if(update.hasCallbackQuery()){

            downloadVideo(update);
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


    private void sendVideoQuality(String messageText, Long chatId){
        String videoId = messageText.split("=")[1];
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> keyboardButtonsRow = new ArrayList<>();
        SortedSet<Integer> quality = new TreeSet<>();
        youtubeService.getVideoInfo(videoId).videoFormats().forEach(videoFormat -> quality.add(videoFormat.height()));

        quality.stream().filter(q -> q >= 360).toList().forEach(q -> {
            InlineKeyboardButton resButton = new InlineKeyboardButton();
            resButton.setText(q.toString());
            resButton.setCallbackData(videoId+"&"+q);
            keyboardButtonsRow.add(resButton);
        });
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(keyboardButtonsRow);
        inlineKeyboardMarkup.setKeyboard(rowList);
        message.setReplyMarkup(inlineKeyboardMarkup);
        message.setText("Select resolution");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
    private void downloadVideo(Update data){

        String videoId = data.getCallbackQuery().getData().split("&")[0];
        String videoQuality = data.getCallbackQuery().getData().split("&")[1];
        Long chatId = data.getCallbackQuery().getMessage().getChatId();
        Integer keyboardId = data.getCallbackQuery().getMessage().getMessageId();
        String filename = "merged_"+videoId+"_"+videoQuality+".mp4";
        String output = "D:/Users/Артем/Desktop/prodavan/merged/"+filename;
        ffmpeg.mergeVA(youtubeService.downloadVideo(videoId,videoQuality),youtubeService.downloadAudio(videoId),output);
        System.out.println("Nice!");


        EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup();
        editMessageReplyMarkup.setChatId(chatId);
        editMessageReplyMarkup.setMessageId(keyboardId);
        try {
            execute(editMessageReplyMarkup);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
        sendMessage(chatId,"http://downloader.ddns.net:5555/downloadFile/"+filename);

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
