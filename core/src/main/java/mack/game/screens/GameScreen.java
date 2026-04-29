package mack.game.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class GameScreen implements Screen {

    private Game game;
    private SpriteBatch batch;
    private BitmapFont font;

    public GameScreen(Game game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        font = new BitmapFont();
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MenuScreen(game));
        }

        Gdx.gl.glClearColor(0.90f, 0.96f, 1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();

        font.setColor(new Color(0.20f, 0.10f, 0.45f, 1));
        font.getData().setScale(2.5f);
        font.draw(batch, "TELA DO JOGO", 230, 400);

        font.getData().setScale(1.3f);
        font.setColor(Color.DARK_GRAY);
        font.draw(batch, "Aqui depois entra a mira, os alvos neurais e o score.", 100, 330);
        font.draw(batch, "Pressione ESC para voltar ao menu.", 170, 280);

        batch.end();
    }

    @Override
    public void resize(int width, int height) {}

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
    }
}