package pr.vexor.telegrambot.babaykakeeper.utils;

public class TextFields {
    
    public static final String START_COMMAND_TEXT = "Привет! Я бот Бабайка-Хранитель \n\n" +
                                    "Доступные команды:\n" +
                                    "/getchatid - узнать ID этого чата\n" +
                                    "/activate  - активировать этот экземпляр бота\n" +
                                    "/standby   - перевести этот экземпляр бота в режим ожидания\n" +
                                    "/status    - посмотреть статус бота";
    
    public static final String STATUS_COMMAND_TEXT = "Статус бота:\n" +
                                    " - Состояние: %s\n" +
                                    " - Экземпляр: %s\n" +
                                    " - Функционал: в разработке\n\n";  

        
    public static final String CHAT_INFO_COMMAND_TEXT = "?Информация о чате:\n" +
                                    "ID: %s\n" +
                                    "Тип: %s\n" +
                                    "Название: %s";  

    
    public static final String ACTIVATION_COMMAND_TEXT = "Активация бота...\n" + 
                                    "Бот активирован и будет теперь обрабатывать посты из канала";
        
    public static final String UNKNOWN_COMMAND_TEXT = "Неизвестная команда\n" +
                                    "Используй /start для списка команд";
    
    public static final String CHECK_POSTS_TEXT = "Проверка постов... (функционал в разработке)";
    
    public static final String STANDBYING_SUCCESS = "Бот переведен в режим ожидания";
    public static final String ACTIVATION_SUCCESS = "Бот активирован и будет теперь обрабатывать посты из канала";
    
    public static final String STANDBYING_ERORR = "Не удалось перевести бота в режим ожидания";
    public static final String ACTIVATION_ERORR = "Не удалось активировать бота";

    
    public static final String ACTIVE_STATUS = "АКТИВНЫЙ";
    public static final String STANDBY_STATUS = "В ОЖИДАНИИ";
    public static final String MESSAGES_TYPE = "Личные сообщения";
    public static final String BUTTON_TEXT = "Обсудить с друзьями";
}
