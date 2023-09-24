
package cafe;

/**
 *
 * @author carol
 */

//Import da biblioteca pi4j v1.4 utilizada para comunicação com o Raspberry Pi 3B
import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
//Import das bibliotecas necessárias para notificações em tempo real via telegram e chamada http
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;



public class Cafe {
    /**
     * Main class
     * @param args
     * @throws InterruptedException 
     */
    public static void main(String[] args) throws InterruptedException {
         gpioListener();
    }
    
    /**
     * Responsável por criar a conexão com o grupo do telegram no qual o bot mandará as notificações de status
     * @param chatId Id do grupo criado
     * @param message Mensagem a ser enviada a cada chamada
     */
    public static void sendToTelegram(String chatId, String message) {
        String urlString = "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s";

        // Token do Telegram do bot cafeteira_bot criado
        String botToken = "6484292851:AAG0odJEkLZ33PbZi7uxrFmkNuDEXuhHdmU";

        urlString = String.format(urlString, botToken, chatId, message);

        try {
            URL url = new URL(urlString);
            URLConnection conn = url.openConnection();
            InputStream is = new BufferedInputStream(conn.getInputStream());
            
        } catch (IOException e) {}
    }
    
    /**
     * Responsável por monitorar o status do Raspberry pi 3B para mandar a mensagem correspondente ao indicado pelo sensor
     */
    public static void gpioListener () {
        final GpioController gpio = GpioFactory.getInstance();
        final GpioPinDigitalInput inputPin1 = gpio.provisionDigitalInputPin(RaspiPin.GPIO_07, PinPullResistance.PULL_DOWN);
        final GpioPinDigitalInput inputPin2 = gpio.provisionDigitalInputPin(RaspiPin.GPIO_00, PinPullResistance.PULL_DOWN);

        inputPin1.setShutdownOptions(true);
        inputPin2.setShutdownOptions(true);


        System.out.println("StatusListener do nível de água do galão iniciado...");

        inputPin1.addListener((GpioPinListenerDigital) (GpioPinDigitalStateChangeEvent event) -> {
            // Monitora mudanças de estado do pin 07
            if (event.getState() == PinState.HIGH) {
                System.out.println("GPIO Pin " + event.getPin() + ": status HIGH (state changed)");
                httpRequestIFTTT("galao_vazio");
                sendToTelegram("-4067332732", "Nível do galão baixo... Convém reabastecer!");
            } else {
                System.out.println("GPIO Pin " + event.getPin() + ": status LOW (state changed)");
                sendToTelegram("-4067332732", "Galão reabastecido!!");
                
            }
        });
        
        System.out.println("StatusListener do nível do reservatorio da cafeteira iniciado...");

        
        inputPin2.addListener((GpioPinListenerDigital) (GpioPinDigitalStateChangeEvent event) -> {
            // Monitora mudanças de estado do pin 00
            if (event.getState() == PinState.HIGH) {
                System.out.println("GPIO Pin " + event.getPin() + ": status HIGH (state changed)");
                httpRequestIFTTT("reservatorio_cafeteira");
            } else {
                System.out.println("GPIO Pin " + event.getPin() + ": status LOW (state changed)");                
            }
        });
        
        System.out.println("Monitorando status do Pin 07 e Pin 00. Aguardando mudanças de estado. Pressione Ctrl+C para parar.");
        
        //Mantém o programa rodando até ser solicitada a parada
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {}    
    }
    
    
    /**
     * Responsável por fazer uma chamada http para adicionar mudanças à planilha de monitoramento de funcionamento
     * @param eventName nome do evento que ocorreu
     */
    public static void httpRequestIFTTT(String eventName){
            
        try {
            String url = "https://maker.ifttt.com/trigger/cafeteira_event/with/key/fSRoSbSiX_JywuqSNYyDUBBDHXWq2Y02k1Yd4Tm1uzC?value1="+eventName;

            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

            connection.setRequestMethod("GET");

            StringBuilder response;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                response = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            System.out.println("Resposta da solicitação GET:");
            System.out.println(response.toString());

            connection.disconnect();
        } catch (IOException e) {}
    }
}





