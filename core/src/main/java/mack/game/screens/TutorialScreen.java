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

public class TutorialScreen implements Screen {

    private Game game;
    private SpriteBatch batch;
    private BitmapFont font;

    private OrthographicCamera camera;
    private Viewport viewport;
    private static final float V_WIDTH  = 800f;
    private static final float V_HEIGHT = 480f;

    public TutorialScreen(Game game) {
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

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
            game.setScreen(new MenuScreen(game));

        Gdx.gl.glClearColor(0.95f, 0.93f, 1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();

        font.setColor(new Color(0.25f, 0.10f, 0.50f, 1));
        font.getData().setScale(2.5f);
        font.draw(batch, "TUTORIAL", 260, 420);

        font.setColor(Color.DARK_GRAY);
        font.getData().setScale(1.3f);
        font.draw(batch, "1. Identifique o alvo neural na tela.",              120, 340);
        font.draw(batch, "2. Ajuste a mira usando o joystick.",                120, 300);
        font.draw(batch, "3. Pressione ESPAÇO para disparar.",                 120, 260);
        font.draw(batch, "4. O jogo avalia precisão, estabilidade e reação.",  120, 220);

        font.setColor(Color.GRAY);
        font.getData().setScale(1f);
        font.draw(batch, "Pressione ESC para voltar ao menu.", 190, 90);

        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
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