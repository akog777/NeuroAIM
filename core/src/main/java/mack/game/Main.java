package mack.game;

import com.badlogic.gdx.Game;
import mack.game.screens.MenuScreen;

public class Main extends Game {

    @Override
    public void create() {
        setScreen(new MenuScreen(this));
    }
}