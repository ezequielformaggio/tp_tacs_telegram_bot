package frba.utn.edu.ar.tp_tacs.api.telegramBot.menuManagement.publishMenu;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.google.common.net.HttpHeaders;

import frba.utn.edu.ar.tp_tacs.api.telegramBot.TelegramBot;
import frba.utn.edu.ar.tp_tacs.api.telegramBot.menuManagement.CommonButtons;
import frba.utn.edu.ar.tp_tacs.api.telegramBot.menuManagement.MenuState;
import frba.utn.edu.ar.tp_tacs.api.telegramBot.sessionManagement.BotSession;
import frba.utn.edu.ar.tp_tacs.api.telegramBot.exceptions.ImageManagingException;
import frba.utn.edu.ar.tp_tacs.domain.entities.Card;
import frba.utn.edu.ar.tp_tacs.enums.ConservationStatus;
import frba.utn.edu.ar.tp_tacs.enums.Game;
import frba.utn.edu.ar.tp_tacs.interfaces.dto.CardDTO;

@Component
public class PublishManager {

    private TelegramBot bot;

    private final WebClient webClient;

    public void setBot(TelegramBot bot) {
        this.bot = bot;
    }
    
    public PublishManager(WebClient webClient) {
        this.webClient = webClient;
    }

    public void displayPublishMenu(BotSession session, Update update) {

        createPublishIfNotExists(session, update);

        if(isPublishState(session, PublishState.NONE)) {

            bot.sendMessage(session, "Perfecto, ¡vamos a publicar una nueva carta!");
            displayGameSelectionMenu(session, update);

        } else if(isPublishState(session, PublishState.AWAITING_GAME_NAME)) {

            receiveGameName(session, update);
            displayNameSelectionMenu(session, update);
            
        } else if (isPublishState(session, PublishState.AWAITING_CARD_NAME)) {

            receiveCardName(session, update);
            displayCardStateSelectionMenu(session, update);
   
        } else if (isPublishState(session, PublishState.AWAITING_CARD_STATE)) {

            receiveCardState(session, update);
            displayImageMenu(session, update);

        } else if (isPublishState(session, PublishState.AWAITING_IMAGES)) {

            receiveCardImages(session, update);
            displayCardPriceMenu(session, update); 

        } else if (isPublishState(session, PublishState.AWAITING_CARD_PRICE)) {

            receiveCardPrice(session, update);
            displayConfirmationPublishMenu(session, update);
            
        } else if (session.getPublish().getPublishState().equals(PublishState.READY_TO_PUBLISH)) {
            managePublishFinalStep(session, update);
        }
    }

    public void displayGameSelectionMenu(BotSession session, Update update) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
    
        for (Game game : Game.values()) {
            InlineKeyboardButton button = InlineKeyboardButton.builder()
                .text(game.toString())
                .callbackData(game.toString())
                .build();
    
            keyboard.add(List.of(button));
        }

        InlineKeyboardMarkup menu = InlineKeyboardMarkup.builder()
            .keyboard(keyboard)
            .build();
    
        session.getPublish().setPublishState(PublishState.AWAITING_GAME_NAME);
        bot.sendMenu(session, "Por favor, indique el juego al que pertenece la carta", menu);
    }

    private void displayNameSelectionMenu(BotSession session, Update update) {
        if(update.hasCallbackQuery()) {
            session.getPublish().setPublishState(PublishState.AWAITING_CARD_NAME);
            bot.sendMessage(session, "¡Bien!, Ahora por favor indique el nombre de la carta");
        }
    }

    private void displayCardStateSelectionMenu(BotSession session, Update update) {
        if(update.getCallbackQuery().getData().equals("correct")) {
            session.getPublish().setPublishState(PublishState.AWAITING_CARD_STATE);
            handleCardStatusSelectionMenu(session, update);
        } 
    }
 
    private void displayImageMenu(BotSession session, Update update) {
        if(update.hasCallbackQuery()) {
            session.getPublish().setPublishState(PublishState.AWAITING_IMAGES);
            bot.sendMessage(session, "¡Bien!, Ahora por favor adjunte las imagenes de la carta y escriba \"listo\"");
            bot.sendMessage(session, "O escriba \"listo\" si desea continuar sin mandar imagenes");
            bot.sendMessage(session, "Si se envio una imagen no deseada, simplemente escriba \"/retry\" y envie las imagenes nuevamente");
        }
    }
    
    private void displayCardPriceMenu(BotSession session, Update update) {
        if (update.hasMessage() && !update.getMessage().hasPhoto() && update.getMessage().getText().toLowerCase().equals("listo")) {
            session.getPublish().setPublishState(PublishState.AWAITING_CARD_PRICE);
            bot.sendMessage(session, "¡Bien!, Ahora por favor escriba el valor estimado de la carta o escriba \"omitir\" para saltear al siguiente paso");
        }
    }

    private void displayConfirmationPublishMenu(BotSession session, Update update) {
        if(!update.hasMessage()) {
            if(update.getCallbackQuery().getData().equals("correct")) {
                // TODO revisar si se tiene que implementar
                //session.getPublish().setPublishState(PublishState.AWAITING_CARDS_FOR_EXCHANGE);
                //bot.sendMessage(session, "¡Bien!, Ahora por favor escriba los id de las cartas que le interesaria recibir a cambio separados por coma para saltear al siguiente paso");

                displayPublishMenu(session);
            } 
        } else if ((update.hasMessage())) {
            if(update.getMessage().getText().toLowerCase().equals("omitir")) {
                displayPublishMenu(session);
            }
        }
    }

    public void displayPublishMenu(BotSession session) {
        session.getPublish().setPublishState(PublishState.READY_TO_PUBLISH);
                Card card = session.getPublish().getCard();
                String price = card.getPrice() == null ? " -" : card.getPrice().toString();
                bot.sendMessage(session, "¡Bien!, vamos a revisar los detalles de la carta 🃏:");
                bot.sendMessage(session, "Nombre: " + card.getName() + "\n" 
                    + "Juego: " + card.getGame().toString() + "\n"
                    + "Estado de conservación: " + card.getState().toString() + "\n"
                    + "Precio: $" + price + "\n");
                if(card.getImages() != null) {
                    for(String fileId : card.getImages()) {
                    SendPhoto photo = new SendPhoto();
                    photo.setChatId(session.getUserId().toString());
                    photo.setPhoto(new InputFile(fileId));  
                    try {
                        bot.execute(photo);
                    } catch (TelegramApiException e) {
                        throw new ImageManagingException("Error al enviar las imagenes " + e);
                    }
                }
                }
                
                bot.sendMenu(session, "¿Desea publicar esta carta?", CommonButtons.displayCorrectOrIncorrectValueButtons());
    }

    private void receiveGameName(BotSession session, Update update) {
        if(update.hasCallbackQuery()) {
            Game game = Game.valueOf(update.getCallbackQuery().getData());
            session.getPublish().getCard().setGame(game);
        }
    }
    
    private void receiveCardName(BotSession session, Update update) {
        if(update.hasMessage()) {
            String name = update.getMessage().getText();
            session.getPublish().getCard().setName(name);
            bot.sendMenu(session, "El valor ingresado, " + name + ", es correcto?", CommonButtons.displayCorrectOrIncorrectValueButtons());
        }
        handleIncorrectValue(session, update);
    }

    private void receiveCardState(BotSession session, Update update) {
        if(update.hasCallbackQuery()) {
            ConservationStatus status = ConservationStatus.valueOf(update.getCallbackQuery().getData());
            session.getPublish().getCard().setState(status);
        }
    }

    private void receiveCardImages(BotSession session, Update update) {
        if(update.hasMessage() && !update.getMessage().hasPhoto() && update.getMessage().getText().equals("/retry")) {
            bot.sendMessage(session, "Perfecto, envie las imagenes nuevamente");
            session.getPublish().getCard().setImages(new ArrayList<>());
        }
        if (update.hasMessage() && update.getMessage().hasPhoto()) {
            List<PhotoSize> photos = update.getMessage().getPhoto();
            PhotoSize largest = photos.get(photos.size() - 1);
            String fileId = largest.getFileId();
            session.getPublish().getCard().getImages().add(fileId);       
        }
    }

    private void receiveCardPrice(BotSession session, Update update) {
        if (update.hasMessage() && update.getMessage().getText().toLowerCase().equals("omitir")) {
            session.getPublish().setPublishState(PublishState.READY_TO_PUBLISH);
        }
        if(update.hasMessage() && !update.getMessage().getText().toLowerCase().equals("omitir")) {
            String price = update.getMessage().getText();
            price = price.replace("$", "").trim();
            if(price.matches("\\d+(\\.\\d+)?")) {
                Double formattedPrice = Double.valueOf(price);
                session.getPublish().getCard().setPrice(formattedPrice);
                bot.sendMenu(session, "El valor ingresado, " + price + ", es correcto?", CommonButtons.displayCorrectOrIncorrectValueButtons());
            } else {
                bot.sendMessage(session, "Por favor ingrese solamente un valor numerico con formato decimal");
            }
        } 
        handleIncorrectValue(session, update);
    }

    private void managePublishFinalStep(BotSession session, Update update) {
        if(update.getCallbackQuery().getData().equals("correct")) {
            try {
                Card card = session.getPublish().getCard();
                Double price = card.getPrice() == null ? Double.valueOf(0) : card.getPrice();
                CardDTO cardDTO = new CardDTO(null, card.getName(), card.getGame(), card.getState(), card.getImages(), price, session.getUserSystemId());
                saveCard(cardDTO, session);
                bot.sendMessage(session, "¡Carta publicada con exito!");
                session.setPublish(null);
                session.setMenuState(MenuState.NONE);
                InlineKeyboardMarkup menu = CommonButtons.displayMenu(session);
                bot.sendMenu(session, "¿Que quieres hacer?", menu);
            } catch( Exception e ) {
                System.out.print(e);
                session.setLogged(false);
            }
        } else {
            bot.sendMessage(session, "Por favor ingrese el valor nuevamente");
        }
    }

    private boolean isPublishState(BotSession session, PublishState state) {
        return session.getPublish().getPublishState().equals(state);
    }

    public void createPublishIfNotExists(BotSession session, Update update) {
        if(session.getPublish() == null) {
            session.setPublish(new Publish());
            session.getPublish().setPublishState(PublishState.NONE);
            session.getPublish().setCard(new Card());
        }
    }
 
    public void handleCardStatusSelectionMenu(BotSession session, Update update) {

        List<InlineKeyboardButton> buttons = new ArrayList<>();

        for(ConservationStatus status : ConservationStatus.values()) {
            buttons.add(InlineKeyboardButton.builder()
            .text(status.toString()).callbackData(status.toString())           
            .build());
        }

        //menu view
        InlineKeyboardMarkup menu = InlineKeyboardMarkup.builder()
            .keyboardRow(buttons).build();

        bot.sendMenu(session, "¡Bien!, Ahora por favor indique el estado de conservación de la carta", menu);
    }

    public void handleIncorrectValue(BotSession session, Update update) {
        if(update.hasCallbackQuery() && update.getCallbackQuery().getData().equals("incorrect")) {
            bot.sendMessage(session, "Por favor ingrese el valor nuevamente");
        }
    }

    public CardDTO saveCard(CardDTO cardDTO, BotSession session) throws IOException {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
    
        builder.part("name", cardDTO.getName());
        builder.part("game", cardDTO.getGame().toString());
        builder.part("state", cardDTO.getState().toString());
        builder.part("price", cardDTO.getPrice());
        builder.part("userId", cardDTO.getUserId());

        for (String fileId : cardDTO.getImages()) {
            String path = System.getenv("JWT_SECRET_KEY_FILE");
            String secret = Files.readString(Path.of(path));
            File downloaded = downloadFromTelegram(fileId, secret);
            if (downloaded != null) {
                builder.part("files", new FileSystemResource(downloaded));
            }
        }
    
        return webClient.post()
            .uri("/cards")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.getAuthToken())
            .body(BodyInserters.fromMultipartData(builder.build()))
            .retrieve()
            .bodyToMono(CardDTO.class)
            .block();
    }

    private File downloadFromTelegram(String fileId, String botToken) {
        try {
            String filePathResponse = WebClient.create()
                .get()
                .uri("https://api.telegram.org/bot" + botToken + "/getFile?file_id=" + fileId)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    
            String filePath = extractFilePathFromJson(filePathResponse);
    
            String fileUrl = "https://api.telegram.org/file/bot" + botToken + "/" + filePath;
            byte[] bytes = WebClient.create()
                .get()
                .uri(fileUrl)
                .retrieve()
                .bodyToMono(byte[].class)
                .block();
    
            File tempFile = File.createTempFile("telegram_", ".jpg");
            Files.write(tempFile.toPath(), bytes);
    
            return tempFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String extractFilePathFromJson(String jsonResponse) throws JSONException {
        JSONObject json = new JSONObject(jsonResponse);
        return json.getJSONObject("result").getString("file_path");
    }
}
