package mack.game.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;

import mack.game.JoystickInput;

public class StoryScreen implements Screen {
    private Game game;
    private SpriteBatch batch;
    private BitmapFont fontText;
    private BitmapFont fontTitle;
    private String storyText;
    private String titleText;
    private int faseHistoria;
    
    private JoystickInput joystick;
    private Thread joystickThread;
    private boolean botaoAnterior = false;
    
    private Texture doctorTexture;

    // --- VARIÁVEIS PARA O EFEITO STAR WARS ---
    private float textY; 
    private float textSpeed = 60f;

    public StoryScreen(Game game, int faseHistoria) {
        this.game = game;
        this.faseHistoria = faseHistoria;
        this.batch = new SpriteBatch();
        
        this.fontTitle = new BitmapFont();
        this.fontTitle.getData().setScale(2.5f);
        this.fontText = new BitmapFont();
        this.fontText.getData().setScale(1.5f);
        
        this.joystick = new JoystickInput(ConfigScreen.portaCOM);
        this.joystickThread = new Thread(joystick);
        this.joystickThread.setDaemon(true);
        this.joystickThread.start();
        
        this.doctorTexture = new Texture(Gdx.files.internal("doctor.png"));

        this.textY = 0f; 

        // --- HISTÓRIAS DE CADA FASE ---
        switch (faseHistoria) {
            case 0:
                titleText = "CÓRTEX NEURAL COMPROMETIDO";
                storyText = "Em um futuro próximo, cientistas descobriram uma dimensão chamada Neurospace, \n" +
                            "onde as conexões neurais humanas são representadas digitalmente.\n\n" +
                            "Uma falha começou a corromper essas conexões, causando perda de controle motor, \n" +
                            "falhas cognitivas e instabilidade nos movimentos.\n\n" +
                            "Para resolver isso, foi criado o NeuroAim, um sistema que permite ao usuário acessar \n" +
                            "o Neurospace e restaurar essas conexões por meio de ações precisas.\n\n" +
                            "Você é um Operador Neural. Elimine as falhas e recupere as conexões.\n" +
                            "O controle exige precisão, estabilidade e foco.";
                break;
                
            case 1:
                titleText = "SISTEMA MOTOR BÁSICO ONLINE";
                storyText = "Bom trabalho, Operador. As conexões primárias foram restabelecidas.\n\n" +
                            "No entanto, o sistema imunológico corrompido do Neurospace detectou sua presença.\n" +
                            "As falhas agora estão se movendo mais rápido para evitar a estabilização.\n\n" +
                            "Prepare-se para movimentos mais agudos.\n" +
                            "Iniciando Nível 2...";
                break;
                
            case 2:
                titleText = "ESTABILIDADE PARCIAL ALCANÇADA";
                storyText = "Apenas o núcleo neural profundo permanece corrompido.\n\n" +
                            "Esta é a raiz da instabilidade. As anomalias estão totalmente erráticas e velozes.\n" +
                            "Esta é a última etapa. Exigimos precisão cirúrgica do seu controle motor.\n\n" +
                            "Mantenha o foco absoluto, Operador.\n" +
                            "Iniciando Sequência Final...";
                break;
                
            case 3:
                titleText = "SISTEMA TOTALMENTE RESTAURADO";
                storyText = "TREINAMENTO CONCLUÍDO COM SUCESSO!\n\n" +
                            "Você eliminou as falhas mais complexas do Neurospace.\n" +
                            "As conexões neurais foram estabilizadas e o controle motor do paciente\n" +
                            "foi 100% restaurado ao mundo real.\n\n" +
                            "Obrigado por jogar NeuroAim.";
                break;
        }
    }

    @Override
    public void show() {}

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        boolean botaoAtual = joystick.botaoPressionado;
        boolean clicouJoystick = botaoAtual && !botaoAnterior;
        botaoAnterior = botaoAtual;

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || 
            Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || 
            clicouJoystick) {
            
            if (faseHistoria < 3) {
                game.setScreen(new GameScreen(game, faseHistoria + 1));
            } else {
                game.setScreen(new MenuScreen(game));
            }
            dispose();
            return;
        }

        // --- ATUALIZA A POSIÇÃO Y DO TEXTO ---
        textY += textSpeed * delta;

        batch.begin();
        
        batch.draw(doctorTexture, 20, 20, 200, 300);

        fontText.setColor(Color.GREEN);
        fontText.draw(batch, "\nVenha, Doutor!" + "\nAjude este paciente!", 230, 280);
        
        fontTitle.setColor(Color.CYAN);
        fontTitle.draw(batch, titleText, 0, Gdx.graphics.getHeight() - 50, Gdx.graphics.getWidth(), Align.center, false);

        fontText.setColor(Color.WHITE);
        fontText.draw(batch, storyText, 100, textY, Gdx.graphics.getWidth() - 200, Align.center, true);

        fontText.setColor(Color.YELLOW);
        String botaoTexto = (faseHistoria < 3) ? "Pressione o Botão ou ESPAÇO para continuar..." : "Pressione o Botão ou ESPAÇO para voltar ao Menu";
        fontText.draw(batch, botaoTexto, 0, 80, Gdx.graphics.getWidth(), Align.center, false);
        
        batch.end();
    }

    @Override
    public void resize(int width, int height) {}

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {
        if (joystick != null) joystick.fechar();
    }

    @Override
    public void dispose() {
        batch.dispose();
        fontText.dispose();
        fontTitle.dispose();
        if (doctorTexture != null) doctorTexture.dispose();
        if (joystick != null) joystick.fechar();
    }
}