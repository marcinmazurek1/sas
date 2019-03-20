package com.github.marcinmazurek1.sas;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.xml.DOMConfigurator;

import com.sas.iom.SAS.ILanguageService;
import com.sas.iom.SAS.ILanguageServicePackage.CarriageControlSeqHolder;
import com.sas.iom.SAS.ILanguageServicePackage.LineTypeSeqHolder;
import com.sas.iom.SASIOMDefs.StringSeqHolder;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * GUI for test connection with SAS server, execute 4GL commands and export
 * results into a file
 *
 * @author Marcin Mazurek
 * @since 1.0
 */
public class App extends Application {

	private Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
	private final double WIDTH = primaryScreenBounds.getWidth() * 2 / 3;
	private final double HEIGHT = primaryScreenBounds.getHeight() * 2 / 3;
	private final String TITLE = "SAS Login";
	private String host = "localhost", username = "username", context = "SASApp", domain = "SASApp";
	private String code = "", source = "sashelp.cars", file = "cars.txt", separator = "|";
	private int port = 8561;

	{
		Properties properties = new Properties();
		try (InputStream in = new FileInputStream("setting.xml")) {
			properties.loadFromXML(in);
			host = properties.getProperty("host", host);
			port = Integer.parseInt(properties.getProperty("port", String.valueOf(port)));
			username = properties.getProperty("username", username);
			context = properties.getProperty("context", context);
			domain = properties.getProperty("domain", domain);
			code = properties.getProperty("code", code);
			source = properties.getProperty("source", source);
			file = properties.getProperty("file", file);
			separator = properties.getProperty("separator", separator);
		} catch (FileNotFoundException e) {
			/* No file is allowed. The program use the default configuration */
		} catch (Exception e) {
			new ErrorForm(e).show();
		}
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		GridPane grid = new GridPane();
		grid.setAlignment(Pos.CENTER);
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(25, 25, 25, 25));

		Label hostLabel = new Label("Host:");
		grid.add(hostLabel, 0, 1);

		TextField hostTextField = new TextField(host);
		grid.add(hostTextField, 1, 1);

		Label portLabel = new Label("Port:");
		grid.add(portLabel, 0, 2);

		TextField portTextField = new TextField(String.valueOf(port));
		grid.add(portTextField, 1, 2);

		Label usernameLabel = new Label("Username:");
		grid.add(usernameLabel, 0, 3);

		TextField usernameTextField = new TextField(username);
		grid.add(usernameTextField, 1, 3);

		Label passwordLabel = new Label("Password:");
		grid.add(passwordLabel, 0, 4);

		PasswordField passwordBox = new PasswordField();
		grid.add(passwordBox, 1, 4);

		Label contextServerLabel = new Label("ServerContext:");
		grid.add(contextServerLabel, 0, 5);

		TextField contextServerTextField = new TextField(context);
		grid.add(contextServerTextField, 1, 5);

		Label authenticationDomainLabel = new Label("AuthenticationDomain:");
		grid.add(authenticationDomainLabel, 0, 6);

		TextField authenticationDomainTextField = new TextField(domain);
		grid.add(authenticationDomainTextField, 1, 6);

		Button btnLogOut = new Button("Log out");
		Button btnSigiIn = new Button("Sign in");
		HBox hbBtn = new HBox(10);
		hbBtn.setAlignment(Pos.BOTTOM_RIGHT);
		hbBtn.getChildren().add(btnSigiIn);
		grid.add(hbBtn, 1, 7);

		btnSigiIn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				ProgressForm pForm = new ProgressForm();

				Task<Void> task = new Task<Void>() {
					@Override
					public Void call() throws Exception {
						Sas.connect(hostTextField.getText(), Integer.parseInt(portTextField.getText()),
								usernameTextField.getText(), passwordBox.getText(), contextServerTextField.getText(),
								authenticationDomainTextField.getText());
						return null;
					}
				};

				pForm.activateProgressBar(task);
				pForm.getDialogStage().show();

				task.setOnFailed(event -> {
					pForm.getDialogStage().close();
					new ErrorForm(task.getException()).show();
				});

				task.setOnSucceeded(event -> {
					pForm.getDialogStage().close();
					BorderPane border = new BorderPane();
					Scene scene = new Scene(border, WIDTH, HEIGHT);
					Stage sasWindow = new Stage();
					sasWindow.setTitle("SAS 4GL");
					sasWindow.setScene(scene);
					sasWindow.show();

					TabPane tabPane = new TabPane();
					TextArea codeArea = new TextArea(code);
					codeArea.setPrefSize(WIDTH, HEIGHT);

					StringBuilder logStringBuilder = new StringBuilder();
					StringBuilder resultStringBuilder = new StringBuilder();

					TextArea logTextArea = new TextArea();
					logTextArea.setEditable(false);
					logTextArea.setPrefSize(WIDTH, HEIGHT);

					Tab logTab = new Tab("Log");
					logTab.setClosable(false);
					logTab.setOnSelectionChanged(new EventHandler<Event>() {
						@Override
						public void handle(Event event) {
							logTextArea.setText(String.valueOf(logStringBuilder));
							border.setCenter(logTextArea);
							border.setBottom(null);
						}
					});

					TextArea resultTextArea = new TextArea();
					resultTextArea.setEditable(false);
					resultTextArea.setPrefSize(WIDTH, HEIGHT);

					Tab resultTab = new Tab("Result");
					resultTab.setClosable(false);
					resultTab.setOnSelectionChanged(new EventHandler<Event>() {
						@Override
						public void handle(Event event) {
							resultTextArea.setText(String.valueOf(resultStringBuilder));
							border.setCenter(resultTextArea);
							border.setBottom(null);
						}
					});

					Tab codeTab = new Tab("Code");
					codeTab.setClosable(false);
					codeTab.setOnSelectionChanged(new EventHandler<Event>() {
						@Override
						public void handle(Event event) {
							border.setCenter(codeArea);
							Button runBtn = new Button("Run");
							runBtn.setOnAction(new EventHandler<ActionEvent>() {
								@Override
								public void handle(ActionEvent event) {
									logStringBuilder.setLength(0);
									resultStringBuilder.setLength(0);

									tabPane.getTabs().remove(logTab);
									tabPane.getTabs().remove(resultTab);

									ProgressForm pForm = new ProgressForm();
									Task<Void> task = new Task<Void>() {
										@Override
										public Void call() throws Exception {
											if (codeArea.getText().isBlank())
												throw new IllegalArgumentException("Code is empty");

											ILanguageService sasLanguage = Sas.workspace().LanguageService();
											CarriageControlSeqHolder logCarriageControlHldr = new CarriageControlSeqHolder();
											LineTypeSeqHolder logLineTypeHldr = new LineTypeSeqHolder();
											StringSeqHolder logHldr = new StringSeqHolder();
											sasLanguage.Submit(codeArea.getText());
											sasLanguage.FlushLogLines(Integer.MAX_VALUE, logCarriageControlHldr,
													logLineTypeHldr, logHldr);
											for (String s : logHldr.value)
												logStringBuilder.append(s).append("\n");

											CarriageControlSeqHolder listCarriageControlHldr = new CarriageControlSeqHolder();
											LineTypeSeqHolder listLineTypeHldr = new LineTypeSeqHolder();
											StringSeqHolder listHldr = new StringSeqHolder();
											sasLanguage.FlushListLines(Integer.MAX_VALUE, listCarriageControlHldr,
													listLineTypeHldr, listHldr);
											for (String s : listHldr.value)
												resultStringBuilder.append(s).append("\n");
											return null;
										}
									};

									pForm.activateProgressBar(task);
									pForm.getDialogStage().show();

									task.setOnFailed(e -> {
										pForm.getDialogStage().close();
										new ErrorForm(task.getException()).show();
									});

									task.setOnSucceeded(e -> {
										pForm.getDialogStage().close();
										if (logStringBuilder.length() > 0) {
											tabPane.getTabs().add(1, logTab);
											tabPane.getSelectionModel().select(1);
										}

										if (resultStringBuilder.length() > 0) {
											tabPane.getTabs().add(1, resultTab);
											tabPane.getSelectionModel().select(1);
										}
									});

									new Thread(task).start();
								}
							});

							final HBox hb2 = new HBox();
							hb2.setAlignment(Pos.CENTER_RIGHT);
							hb2.getChildren().add(runBtn);
							border.setBottom(hb2);
						}
					});

					Tab exportTab = new Tab("Export");
					exportTab.setClosable(false);
					exportTab.setOnSelectionChanged(new EventHandler<Event>() {
						@Override
						public void handle(Event event) {
							GridPane grid = new GridPane();
							grid.setAlignment(Pos.CENTER);
							grid.setHgap(10);
							grid.setVgap(10);
							grid.setPadding(new Insets(25, 25, 25, 25));

							Label sourceLabel = new Label("Source:");
							grid.add(sourceLabel, 0, 1);

							TextField sourceTextField = new TextField(source);
							sourceTextField.setPrefWidth(WIDTH / 2);
							grid.add(sourceTextField, 1, 1);

							Label targetLabel = new Label("Target:");
							grid.add(targetLabel, 0, 2);

							TextField fileTextField = new TextField(file);
							fileTextField.setPrefWidth(WIDTH / 2);
							grid.add(fileTextField, 1, 2);

							Button chooseBtn = new Button("Choose");
							chooseBtn.setOnAction(new EventHandler<ActionEvent>() {
								@Override
								public void handle(ActionEvent event) {
									FileChooser fileChooser = new FileChooser();
									FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
											"TXT files (*.txt)", "*.txt");
									fileChooser.getExtensionFilters().add(extFilter);
									fileTextField.setText(String.valueOf(fileChooser.showSaveDialog(sasWindow)));
								}
							});
							grid.add(chooseBtn, 1, 3);
							border.setCenter(grid);

							Button exportBtn = new Button("Export");
							exportBtn.setOnAction(new EventHandler<ActionEvent>() {
								@Override
								public void handle(ActionEvent event) {
									ProgressForm pForm = new ProgressForm();
									Task<Void> task = new Task<Void>() {
										@Override
										public Void call() throws Exception {
											Connection conn = Sas.connection();
											Statement stmt = conn.createStatement();
											ResultSet rs = stmt
													.executeQuery("SELECT * FROM " + sourceTextField.getText());

											ResultSetMetaData rsmd = rs.getMetaData();
											int size = rsmd.getColumnCount();

											List<String> lines = new ArrayList<String>();
											StringBuilder text = new StringBuilder();
											for (int i = 1; i <= size; i++) {
												String str = rsmd.getColumnLabel(i) != null
														&& !rsmd.getColumnLabel(i).isBlank() ? rsmd.getColumnLabel(i)
																: rsmd.getColumnName(i);
												if (str != null)
													str = str.trim();
												text.append(str).append(separator);
											}
											int length = separator.length();
											text.setLength(text.length() - length);
											lines.add(String.valueOf(text));
											text.setLength(0);

											while (rs.next()) {
												for (int i = 1; i <= size; i++) {
													String str = rs.getString(i);
													if (str != null)
														str = str.trim();
													text.append(str).append(separator);
												}
												text.setLength(text.length() - length);
												lines.add(String.valueOf(text));
												text.setLength(0);
											}
											String file = fileTextField.getText();
											if (!file.endsWith(".txt"))
												file += ".txt";
											Files.write(Paths.get(file), lines);
											return null;
										}
									};

									pForm.activateProgressBar(task);
									pForm.getDialogStage().show();

									task.setOnFailed(e -> {
										pForm.getDialogStage().close();
										new ErrorForm(task.getException()).show();
									});

									task.setOnSucceeded(e -> {
										pForm.getDialogStage().close();
										new MessageForm("Export", "File has been exported").show();
									});

									new Thread(task).start();
								}
							});
							final HBox hb2 = new HBox();
							hb2.setAlignment(Pos.CENTER_RIGHT);
							hb2.getChildren().add(exportBtn);
							border.setBottom(hb2);
						}
					});

					tabPane.getTabs().addAll(codeTab, exportTab);
					border.setTop(tabPane);

					final HBox hb2 = new HBox();
					hb2.setAlignment(Pos.CENTER_RIGHT);
					Button runBtn = new Button("Run");
					hb2.getChildren().add(runBtn);

					hbBtn.getChildren().remove(btnSigiIn);
					hbBtn.getChildren().add(btnLogOut);
				});

				new Thread(task).start();
			}

		});

		btnLogOut.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				stop();
				hbBtn.getChildren().remove(btnLogOut);
				hbBtn.getChildren().add(btnSigiIn);
			}
		});

		Scene scene = new Scene(grid, 400, 300);
		primaryStage.setTitle(TITLE);
		primaryStage.setScene(scene);
		primaryStage.show();
	}

	@Override
	public void stop() {
		try {
			Sas.close();
		} catch (Exception e) {
			new ErrorForm(e).show();
		}
	}

	public static class ErrorForm {
		private final double WIDTH = 800;
		private final double HEIGHT = 400;
		private final Alert alert = new Alert(AlertType.ERROR);

		{
			alert.setResizable(true);
			alert.setHeaderText(null);
		}

		public ErrorForm(Throwable throwable) {
			Writer wr = new StringWriter();
			throwable.printStackTrace(new PrintWriter(wr));

			TextArea textArea = new TextArea(String.valueOf(wr));
			textArea.setPrefSize(WIDTH, HEIGHT);
			textArea.setEditable(false);

			VBox box = new VBox();
			Label label = new Label("Stack Trace:");

			box.getChildren().addAll(label, textArea);
			alert.getDialogPane().setContent(box);
		}

		public void show() {
			alert.showAndWait();
		}

	}

	public static class MessageForm {
		private final Alert alert = new Alert(AlertType.INFORMATION);

		{
			alert.setResizable(true);
			alert.setHeaderText(null);
		}

		public MessageForm(String title, String message) {
			alert.setTitle(title);
			alert.setContentText(message);
		}

		public void show() {
			alert.showAndWait();
		}
	}

	public static class ProgressForm {
		private final double WIDTH = 400;
		private final double HEIGHT = 200;

		private final Stage dialogStage;
		private final ProgressIndicator pin = new ProgressIndicator();

		public ProgressForm() {
			dialogStage = new Stage();
			dialogStage.setWidth(WIDTH);
			dialogStage.setHeight(HEIGHT);
			dialogStage.initStyle(StageStyle.UTILITY);
			dialogStage.setResizable(false);
			dialogStage.setTitle("Progress");
			dialogStage.initModality(Modality.APPLICATION_MODAL);
			pin.setProgress(-1F);

			final HBox hb = new HBox();
			hb.setSpacing(5);
			hb.setAlignment(Pos.CENTER);
			hb.getChildren().addAll(pin);

			Scene scene = new Scene(hb);
			dialogStage.setScene(scene);
		}

		public void activateProgressBar(final Task<?> task) {
			pin.progressProperty().bind(task.progressProperty());
			dialogStage.show();
		}

		public Stage getDialogStage() {
			return dialogStage;
		}
	}

	public static void main(String[] args) {
		Path path = Paths.get("log4j.xml");
		try {
			if (!Files.exists(path))
				throw new FileNotFoundException(String.valueOf(path));
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		DOMConfigurator.configure(String.valueOf(path));
		launch(args);
	}
}
