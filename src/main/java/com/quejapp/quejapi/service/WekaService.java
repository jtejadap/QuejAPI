package com.quejapp.quejapi.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.classifiers.Evaluation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class WekaService {
    private static final Logger logger = LoggerFactory.getLogger(WekaService.class);

    private NaiveBayes classifier;
    private Instances trainingData;

    @PostConstruct
    public void init() {
        try {
            logger.info("Iniciando entrenamiento del modelo al arranque...");
            trainModel();
            logger.info("Modelo entrenado exitosamente al arranque");
        } catch (Exception e) {
            logger.error("Error al entrenar el modelo en el arranque", e);
            throw new RuntimeException("No se pudo inicializar el modelo", e);
        }
    }

    public String trainModel() throws Exception {
        // Cargar el dataset desde resources
        ClassPathResource resource = new ClassPathResource("dataset.quejapp.arff");
        DataSource source = new DataSource(resource.getInputStream());
        trainingData = source.getDataSet();

        // Establecer el atributo de clase (última columna)
        if (trainingData.classIndex() == -1) {
            trainingData.setClassIndex(trainingData.numAttributes() - 1);
        }

        // Crear y entrenar el clasificador NaiveBayes
        classifier = new NaiveBayes();
        classifier.buildClassifier(trainingData);

        return "Modelo entrenado exitosamente con " + trainingData.numInstances() + " instancias";
    }

    public String predict(String tipo, String canal, String dia, String mes, Boolean mode) throws Exception {
        if (classifier == null || trainingData == null) {
            throw new IllegalStateException("El modelo no está entrenado");
        }

        // Verificar que los atributos existen
        String[] attributeNames = { "tipo", "canal", "dia", "mes" };
        for (String attrName : attributeNames) {
            if (trainingData.attribute(attrName) == null) {
                throw new IllegalArgumentException("Atributo no encontrado: " + attrName);
            }
        }

        // Crear una nueva instancia para predicción
        DenseInstance newInstance = new DenseInstance(trainingData.numAttributes());
        newInstance.setDataset(trainingData);

        // Establecer los valores de los atributos
        System.out.println("Atributos disponibles:");
        for (int i = 0; i < trainingData.numAttributes(); i++) {
            System.out.println("  " + i + ": " + trainingData.attribute(i).name());
        }

        newInstance.setValue(trainingData.attribute("tipo"), tipo);
        newInstance.setValue(trainingData.attribute("canal"), canal);
        newInstance.setValue(trainingData.attribute("dia"), dia);
        newInstance.setValue(trainingData.attribute("mes"), mes);

        // Hacer la predicción
        double prediction = classifier.classifyInstance(newInstance);
        String predictedClass = trainingData.classAttribute().value((int) prediction);

        // Obtener la distribución de probabilidades
        double[] distribution = classifier.distributionForInstance(newInstance);

        if (mode != null && mode) {
            return predictedClass;
        }

        StringBuilder result = new StringBuilder();
        result.append("Predicción: ").append(predictedClass).append("\n");
        result.append("Probabilidades:\n");
        for (int i = 0; i < distribution.length; i++) {
            result.append("  ").append(trainingData.classAttribute().value(i))
                    .append(": ").append(String.format("%.4f", distribution[i])).append("\n");
        }

        return result.toString();
    }

    public String evaluateModel() throws Exception {
        if (classifier == null || trainingData == null) {
            throw new IllegalStateException("El modelo no está entrenado");
        }

        // Evaluación con validación cruzada de 10 folds
        Evaluation eval = new Evaluation(trainingData);
        eval.crossValidateModel(classifier, trainingData, 10, new Random(1));

        StringBuilder result = new StringBuilder();
        result.append("=== Evaluación del Modelo ===\n\n");
        result.append(eval.toSummaryString());
        result.append("\n").append(eval.toClassDetailsString());
        result.append("\n").append(eval.toMatrixString());

        return result.toString();
    }
}
