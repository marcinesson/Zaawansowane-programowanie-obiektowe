import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;

public class MainFrame {
    private JFrame frame;
    // ścieżka do katalogu z plikami tekstowymi
    private static final String DIR_PATH = "files";
    // określa ile najczęściej występujących wyrazów bierzemy pod uwagę
    private final int liczbaWyrazowStatystyki;
    private final AtomicBoolean fajrant;
    private final int liczbaProducentow;
    private final int liczbaKonsumentow;
    // pula wątków – obiekt klasy ExecutorService, który zarządza tworzeniem nowych oraz wykonuje 'recykling' zakończonych wątków
    private ExecutorService executor;
    // lista obiektów klasy Future, dzięki którym mamy możliwość nadzoru pracy wątków producenckich
    private List<Future<?>> producentFuture;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    MainFrame window = new MainFrame();
                    window.frame.pack();
                    window.frame.setAlwaysOnTop(true);
                    window.frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public MainFrame() {
        liczbaWyrazowStatystyki = 10;
        fajrant = new AtomicBoolean(false);
        liczbaProducentow = 1;
        liczbaKonsumentow = 2;
        executor = Executors.newFixedThreadPool(liczbaProducentow + liczbaKonsumentow);
        producentFuture = new ArrayList<>();
        initialize();
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        frame = new JFrame("Statystyka Wyrazów (Producent-Konsument)");
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                executor.shutdownNow();
            }
        });
        frame.setBounds(100, 100, 450, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        frame.getContentPane().add(panel, BorderLayout.NORTH);

        JButton btnStop = new JButton("Stop");
        btnStop.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fajrant.set(true);
                for (Future<?> f : producentFuture) {
                    f.cancel(true);
                }
            }
        });

        JButton btnStart = new JButton("Start");
        btnStart.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                getMultiThreadedStatistics();
            }
        });

        JButton btnZamknij = new JButton("Zamknij");
        btnZamknij.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                executor.shutdownNow();
                frame.dispose();
            }
        });

        panel.add(btnStart);
        panel.add(btnStop);
        panel.add(btnZamknij);
    }

    private void getMultiThreadedStatistics() {
        for (Future<?> f : producentFuture) {
            if (!f.isDone()) {
                JOptionPane.showMessageDialog(frame, "Nie można uruchomić nowego zadania! Przynajmniej jeden producent nadal działa!", "OSTRZEŻENIE", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        fajrant.set(false);
        producentFuture.clear();
        final BlockingQueue<Optional<Path>> kolejka = new LinkedBlockingQueue<>(liczbaKonsumentow);
        final int przerwa = 10; // Czas przerwy przed ponownym skanowaniem katalogu (skróciłem dla łatwiejszego testowania)

        Runnable producent = () -> {
            final String name = Thread.currentThread().getName();
            String info = String.format("PRODUCENT %s URUCHOMIONY ...", name);
            System.out.println(info);

            while (!Thread.currentThread().isInterrupted()) {
                if (fajrant.get()) {
                    // TODO: przekazanie poison pills konsumentom i zakończenia działania
                    try {
                        for (int i = 0; i < liczbaKonsumentow; i++) {
                            kolejka.put(Optional.empty());
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    break; // Wychodzimy z pętli - kończymy pracę producenta
                } else {
                    // TODO: Wyszukiwanie plików *.txt i wstawianie do kolejki
                    Path dir = Paths.get(DIR_PATH);
                    if (Files.exists(dir)) {
                        try {
                            // PathMatcher posłuży nam do filtrowania wyłącznie plików z rozszerzeniem .txt
                            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/*.txt");

                            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                                @Override
                                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                                    if (matcher.matches(path)) {
                                        try {
                                            // Jeśli kolejka pełna, put() zablokuje producenta aż zwolni się miejsce
                                            kolejka.put(Optional.of(path));
                                        } catch (InterruptedException e) {
                                            Thread.currentThread().interrupt();
                                            return FileVisitResult.TERMINATE; // Przerwanie iterowania po plikach
                                        }
                                    }
                                    return FileVisitResult.CONTINUE;
                                }
                            });
                        } catch (IOException e) {
                            System.err.println("Błąd podczas odczytu drzewa katalogów: " + e.getMessage());
                        }
                    } else {
                        System.out.println("Katalog '" + DIR_PATH + "' nie istnieje!");
                    }
                }

                info = String.format("Producent %s ponownie sprawdzi katalogi za %d sekund", name, przerwa);
                System.out.println(info);

                try {
                    TimeUnit.SECONDS.sleep(przerwa);
                } catch (InterruptedException e) {
                    info = String.format("Przerwa producenta %s przerwana!", name);
                    System.out.println(info);
                    if (!fajrant.get()) Thread.currentThread().interrupt();
                }
            }
            info = String.format("PRODUCENT %s SKOŃCZYŁ PRACĘ", name);
            System.out.println(info);
        };

        Runnable konsument = () -> {
            final String name = Thread.currentThread().getName();
            String info = String.format("KONSUMENT %s URUCHOMIONY ...", name);
            System.out.println(info);

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // TODO: pobieranie ścieżki i tworzenie statystyki
                    Optional<Path> optPath = kolejka.take(); // take() blokuje, jeśli kolejka pusta

                    if (optPath.isPresent()) {
                        Path path = optPath.get();
                        System.out.println("-> " + name + " przetwarza plik: " + path.getFileName());

                        Map<String, Long> statystyka = getLinkedCountedWords(path, liczbaWyrazowStatystyki);
                        System.out.println("   Wynik [" + path.getFileName() + "]: " + statystyka);
                    } else {
                        // Otrzymano poison pill - zakańczamy pracę
                        break;
                    }
                } catch (InterruptedException e) {
                    info = String.format("Oczekiwanie konsumenta %s na nowy element z kolejki przerwane!", name);
                    System.out.println(info);
                    Thread.currentThread().interrupt();
                }
            }
            info = String.format("KONSUMENT %s ZAKOŃCZYŁ PRACĘ", name);
            System.out.println(info);
        };

        // uruchamianie wszystkich wątków-producentów
        for (int i = 0; i < liczbaProducentow; i++) {
            Future<?> pf = executor.submit(producent);
            producentFuture.add(pf);
        }
        // uruchamianie wszystkich wątków-konsumentów
        for (int i = 0; i < liczbaKonsumentow; i++) {
            executor.execute(konsument);
        }
    }

    private Map<String, Long> getLinkedCountedWords(Path path, int wordsLimit) {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            return reader.lines()
                    // Podział linii na słowa za pomocą \s+ i spłaszczenie strumienia
                    .flatMap(line -> Arrays.stream(line.split("\\s+")))
                    // Konwersja na małe litery zrobiona na początku ułatwia nam regexy poniżej
                    .map(String::toLowerCase)
                    // Wycięcie znaków interpunkcyjnych, pozostawiamy tylko litery (polskie też) i cyfry
                    .map(word -> word.replaceAll("[^a-z0-9ąęóśćżńź]", ""))
                    // Filtrujemy by zostały tylko wyrazy mające minimum 3 znaki
                    .filter(word -> word.matches("[a-z0-9ąęóśćżńź]{3,}"))
                    // Grupowanie względem wyrazu i liczenie częstotliwości
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                    .entrySet().stream()
                    // Sortowanie mapy względem wartości (malejąco)
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    // Limitowanie wyników
                    .limit(wordsLimit)
                    // Umieszczenie wyników w LinkedHashMap, aby zachować posortowaną kolejność
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (k, v) -> { throw new IllegalStateException(String.format("Błąd! Duplikat klucza %s.", k)); },
                            LinkedHashMap::new
                    ));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}