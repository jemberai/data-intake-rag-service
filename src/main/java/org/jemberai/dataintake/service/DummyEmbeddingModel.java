package org.jemberai.dataintake.service;
/*
 * Created by Ashok Kumar Pant
 * Email: asokpant@gmail.com
 * Created on 04/10/2024.
 */

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.DimensionAwareEmbeddingModel;
import dev.langchain4j.model.embedding.TokenCountEstimator;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class DummyEmbeddingModel extends DimensionAwareEmbeddingModel implements TokenCountEstimator {
    private final Integer dimensions;

    public DummyEmbeddingModel(Integer dimensions) {
        this.dimensions = dimensions;
    }

    protected Integer knownDimension() {
        return this.dimensions;
    }

    public String modelName() {
        return "JemberDummyModel";
    }

    public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
        List<String> texts = textSegments.stream().map(TextSegment::text).collect(Collectors.toList());
        return this.embedTexts(texts);
    }

    private Response<List<Embedding>> embedTexts(List<String> texts) {
        Random random = new Random();
        List<Embedding> embeddings = new ArrayList<>();
        for (String text : texts) {

            float[] vector = new float[this.dimensions];
            for (int i = 0; i < vector.length; i++) {
                vector[i] = random.nextFloat();
            }
            Embedding embedding = Embedding.from(vector);
            embeddings.add(embedding);
        }
        return Response.from(embeddings, new TokenUsage(random.nextInt(100), random.nextInt(100)));
    }

    public int estimateTokenCount(String text) {
        return new Random().nextInt(1000);
    }

}
