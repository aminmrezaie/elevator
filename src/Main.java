import simulation.Config;
import simulation.Simulation;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== شبیه‌سازی آسانسورهای دانشگاه صنعتی شریف ===");
        System.out.println();

        Scanner scanner = new Scanner(System.in);
        System.out.print("لطفا تعداد طبقات ساختمان (M) را وارد کنید: ");
        int m = scanner.nextInt();

        System.out.print("لطفا تعداد کل آسانسورها (N) را وارد کنید: ");
        int n = scanner.nextInt();

        Config cfg = new Config();
        cfg.numFloors = m;

        // Distribute elevators roughly (at least 1 of each if N >= 3)
        if (n >= 3) {
            cfg.numGeneralElevators = n - 2;
            cfg.numFacultyElevators = 1;
            cfg.numFreightElevators = 1;
        } else {
            cfg.numGeneralElevators = n;
            cfg.numFacultyElevators = 0;
            cfg.numFreightElevators = 0;
        }

        Simulation.init(cfg);
        Simulation.getInstance().run();
    }
}
