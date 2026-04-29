package mack.game.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class MenuScreen implements Screen {

    private Game game;
    private SpriteBatch batch;
    private BitmapFont font;

    private String[] opcoes = {
            "JOGAR",
            "CONFIGURAÇÕES",
            "TUTORIAL",
            "SAIR"
    };

    private int opcaoSelecionada = 0;

    public MenuScreen(Game game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        font = new BitmapFont();
    }

    @Override
    public void render(float delta) {
        verificarEntrada();

        Gdx.gl.glClearColor(0.95f, 0.93f, 1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();

        font.setColor(new Color(0.25f, 0.10f, 0.50f, 1));
        font.getData().setScale(3f);
        font.draw(batch, "NEUROAIM", 230, 430);

        font.setColor(Color.DARK_GRAY);
        font.getData().setScale(1.3f);
        font.draw(batch, "Treine sua mente. Aprimore seu foco.", 165, 385);

        font.getData().setScale(2f);

        for (int i = 0; i < opcoes.length; i++) {
            if (i == opcaoSelecionada) {
                font.setColor(Color.PURPLE);
                font.draw(batch, "> " + opcoes[i], 250, 300 - i * 55);
            } else {
                font.setColor(Color.DARK_GRAY);
                font.draw(batch, opcoes[i], 285, 300 - i * 55);
            }
        }

        font.setColor(Color.GRAY);
        font.getData().setScale(1f);
        font.draw(batch, "Use W/S ou Setas para navegar | ENTER para selecionar", 115, 60);

        batch.end();
    }

    private void verificarEntrada() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) ||
            Gdx.input.isKeyJustPressed(Input.Keys.S)) {

            opcaoSelecionada++;

            if (opcaoSelecionada >= opcoes.length) {
                opcaoSelecionada = 0;
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) ||
            Gdx.input.isKeyJustPressed(Input.Keys.W)) {

            opcaoSelecionada--;

            if (opcaoSelecionada < 0) {
                opcaoSelecionada = opcoes.length - 1;
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            selecionarOpcao();
        }
    }

    private void selecionarOpcao() {
        switch (opcaoSelecionada) {
            case 0:
                game.setScreen(new GameScreen(game));
                break;

            case 1:
                game.setScreen(new ConfigScreen(game));
                break;

            case 2:
                game.setScreen(new TutorialScreen(game));
                break;

            case 3:
                Gdx.app.exit();
                break;
        }
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