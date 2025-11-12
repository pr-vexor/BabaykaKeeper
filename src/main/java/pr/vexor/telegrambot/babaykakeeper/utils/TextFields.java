package pr.vexor.telegrambot.babaykakeeper.utils;

public class TextFields {
    
    public static final String START_COMMAND_TEXT = """
                                                    Привет! Я бот Бабайка-Хранитель 
                                                    
                                                    Доступные команды:
                                                    /getchatid  - узнать ID этого чата
                                                    /activate    - активировать бота
                                                    /standby    - перевести бота в режим ожидания
                                                    /status        - посмотреть статус бота
                                                    """;
    
    public static final String STATUS_COMMAND_TEXT = """
                                                     Статус бота:
                                                      - Состояние: %s
                                                      - Экземпляр: %s
                                                      - Функционал: в разработке
                                                     """;
    
    public static final String CHAT_INFO_COMMAND_TEXT = """
                                                        Информация о чате:
                                                         - ID: %s
                                                         - Тип: %s
                                                         - Название: %s
                                                        """;
    
    public static final String UNKNOWN_COMMAND_TEXT = """
                                                      Неизвестная команда
                                                      Используй /start для списка команд
                                                      """;
    
    public static final String STANDBYING_SUCCESS = "Бот переведён в режим ожидания";
    public static final String STANDBYING_ERORR = "Не удалось перевести бота в режим ожидания";
    public static final String ACTIVATION_SUCCESS = "Бот активирован и будет теперь обрабатывать посты из канала";
    public static final String ACTIVATION_ERORR = "Не удалось активировать бота";
    public static final String COMMAND_HANDLE_IGNORE = "Бот не активен, команда не обработана";
    public static final String ALREADY_ACTIVE = " Бот уже активен";
    public static final String ALREADY_STANDBY = " Бот уже переведён в режим ожидания";
    public static final String COMMAND_HANDLE_NON_ADMIN_IGNORE = "Бот обрабатывает только команды админа";

    public static final String ACTIVE_STATUS = "АКТИВНЫЙ";
    public static final String STANDBY_STATUS = "В ОЖИДАНИИ";
    public static final String MESSAGES_TYPE = "Личные сообщения";
    
    public static final String BUTTON_LINK_HTML_FORMAT = "\n\n#всем_чай <a href=\"%s\">%s</a>";
    public static final String MESSAGE_LINK_FORMAT = "https://t.me/c/%s/%s";
}
