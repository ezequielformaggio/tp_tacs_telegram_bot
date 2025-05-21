package frba.utn.edu.ar.tp_tacs.api.telegramBot.menuManagement.publishMenu;

import frba.utn.edu.ar.tp_tacs.domain.entities.Card;
import lombok.Data;

@Data
public class Publish {
    private Card card;
    private PublishState publishState;
}
