package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.example.service.DatabaseInitializer;

import java.io.InputStream;
import java.util.Objects;

/**
 * Головний клас додатку для графіку змін працівників.
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            System.out.println("🚀 Ініціалізація додатку з реальними графіками...");

            // Ініціалізація БД
            DatabaseInitializer.initializeDatabaseWithRealData();

            // Завантаження FXML
            FXMLLoader loader = new FXMLLoader();
            String fxmlPath = "/org/example/ui/MainWindow.fxml";

            InputStream fxmlStream = getClass().getResourceAsStream(fxmlPath);
            if (fxmlStream == null) {
                throw new RuntimeException("FXML файл не знайдено: " + fxmlPath);
            }
            fxmlStream.close();

            Parent root = FXMLLoader.load(Objects.requireNonNull(
                    getClass().getResource(fxmlPath)));

            System.out.println("✅ FXML успішно завантажено");

            Scene scene = new Scene(root, 1300, 750);

            // Додавання CSS
            try {
                String cssPath = "/org/example/ui/styles.css";
                scene.getStylesheets().add(Objects.requireNonNull(
                        getClass().getResource(cssPath)).toExternalForm());
                System.out.println("✅ CSS завантажено");
            } catch (Exception e) {
                System.err.println("⚠️ CSS не знайдено: " + e.getMessage());
            }

            // Налаштування вікна
            primaryStage.setTitle("Графік змін працівників - Полтававодоканал (Січень 2026)");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(700);

            // Додавання іконки
            try {
                InputStream iconStream = getClass().getResourceAsStream("/org/example/ui/icon.png");
                if (iconStream != null) {
                    Image icon = new Image(iconStream);
                    primaryStage.getIcons().add(icon);
                    System.out.println("✅ Іконка завантажена");
                }
            } catch (Exception e) {
                System.err.println("⚠️ Іконка не знайдена");
            }

            // Показ вікна
            primaryStage.show();
            System.out.println("🎉 Додаток запущено з реальними графіками!");

        } catch (Exception e) {
            System.err.println("❌ Помилка запуску: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}