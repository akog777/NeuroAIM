package mack.game.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class ConfigScreen implements Screen {

    private Game game;
    private SpriteBatch batch;
    private BitmapFont font;

    private OrthographicCamera camera;
    private Viewport viewport;
    private static final float V_WIDTH  = 800f;
    private static final float V_HEIGHT = 480f;

    private boolean somLigado  = true;
    private int     dificuldade = MenuScreen.getDificuldade();

    public ConfigScreen(Game game) {
        this.game = game;
    }

    @Override
    public void show() {
        camera   = new OrthographicCamera();
        viewport = new FitViewport(V_WIDTH, V_HEIGHT, camera);
        viewport.apply();
        camera.position.set(V_WIDTH / 2f, V_HEIGHT / 2f, 0);

        batch = new SpriteBatch();
        font  = new BitmapFont();
    }

    @Override
    public void render(float delta) {
        camera.update();
        batch.setProjectionMatrix(camera.combined);

        verificarEntrada();

        Gdx.gl.glClearColor(0.96f, 0.94f, 1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();

        font.setColor(new Color(0.25f, 0.10f, 0.50f, 1));
        font.getData().setScale(2.5f);
        font.draw(batch, "CONFIGURAÇÕES", 200, 410);

        font.getData().setScale(1.5f);
        font.setColor(Color.DARK_GRAY);
        font.draw(batch, "Som: " + (somLigado ? "Ligado" : "Desligado"), 230, 320);
        font.draw(batch, "Dificuldade: " + getNomeDificuldade(), 230, 270);

        font.getData().setScale(1f);
        font.setColor(Color.GRAY);
        font.draw(batch, "Pressione ESPAÇO para ligar/desligar som", 150, 150);
        font.draw(batch, "Pressione D para mudar dificuldade",       170, 120);
        font.draw(batch, "Pressione ESC para voltar",                220,  90);

        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    private void verificarEntrada() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE))
            somLigado = !somLigado;

        if (Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            dificuldade = (dificuldade % 3) + 1;
            MenuScreen.setDificuldade(dificuldade);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
            game.setScreen(new MenuScreen(game));
    }

    private String getNomeDificuldade() {
        switch (dificuldade) {
            case 1:  return "Inicial";
            case 2:  return "Intermediário";
            default: return "Avançado";
        }
    }

    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   {}

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
    }
}