package frba.utn.edu.ar.tp_tacs.api.telegramBot.sessionManagement;

import frba.utn.edu.ar.tp_tacs.api.telegramBot.menuManagement.MenuState;
import frba.utn.edu.ar.tp_tacs.api.telegramBot.menuManagement.publishMenu.Publish;
import frba.utn.edu.ar.tp_tacs.api.telegramBot.menuManagement.searchMenu.FilteredSearch;
import lombok.Data;
import lombok.Setter;

@Data
@Setter
public class BotSession {
    private boolean logged;
    private String username;
    private String password;
    private Long userId;
    private SessionState state = SessionState.START;
    private MenuState menuState = MenuState.NONE;
    private FilteredSearch filteredSearch;
    private String authToken;
    private Publish publish;
    private String userSystemId;

    public BotSession(Long userId) {
        this.userId = userId;
        this.logged = false;
    }

    public void discardOngoingOperations() {
        this.menuState = MenuState.NONE;
        this.publish = null;
        this.filteredSearch = null;
    }
}
