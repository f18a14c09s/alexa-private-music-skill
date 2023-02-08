package f18a14c09s.util;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter(AccessLevel.PRIVATE)
public class CsvFileWriter implements CsvWriter, AutoCloseable {
    private FileWriter fileWriter;
    private BufferedWriter writer;
    private List<String> header = new ArrayList<>();

    public CsvFileWriter() {
    }

    public static CsvFileWriter from(File file, List<String> header) throws IOException {
        CsvFileWriter csvWriter = new CsvFileWriter();
        FileWriter fileWriter = new FileWriter(file);
        csvWriter.setFileWriter(fileWriter);
        csvWriter.setWriter(new BufferedWriter(fileWriter));
        csvWriter.getHeader().addAll(header);
        return csvWriter;
    }

    @Override
    public void close() throws IOException {
        try {
            writer.close();
        } finally {
            fileWriter.close();
        }
    }
}
