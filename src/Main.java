import simulation.Config;
import simulation.Simulation;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== شبیه‌سازی آسانسورهای دانشگاه صنعتی شریف ===");
        System.out.println();

        Config cfg = new Config();
        new Simulation(cfg).run();
    }
}

