package com.mzheltoukhov.questpistols.repository;

import com.mzheltoukhov.questpistols.model.Game;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.addFields;
import static org.springframework.data.mongodb.core.aggregation.ArrayOperators.Size.lengthOfArray;

public class GameRepositoryImpl implements GameRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public List<String> findGamesToFinishIds() {
        return mongoTemplate.aggregate(
                newAggregation(
                        match(Criteria.where("status").is("STARTED").and("startTime").ne(null)),
                        a -> a.getMappedObject(Document.parse("{\n" +
                                "        $lookup: {\n" +
                                "            from: 'quests',\n" +
                                "                    localField: 'questName',\n" +
                                "                    foreignField: 'name',\n" +
                                "                    as: 'quest'\n" +
                                "        }\n" +
                                "    }")),
                        a -> a.getMappedObject(Document.parse("{\n" +
                                "        $unwind: {\n" +
                                "            path: \"$quest\"\n" +
                                "        }\n" +
                                "    }")),
                        a -> a.getMappedObject(Document.parse("{\n" +
                                "        $addFields: {\n" +
                                "            \"durationMinutes\": \"$quest.durationMinutes\"" +
                                "        }\n" +
                                "    }")),
                        match(Criteria.where("durationMinutes").ne(null)),
//                        a -> a.getMappedObject(Document.parse("{\n" +
//                                "        $project: {\n" +
//                                "            \"startTime\": 1,\n" +
//                        "                    \"durationMinutes\": 1\n" +
//                                "        }\n" +
//                                "    }")),
                        a -> a.getMappedObject(Document.parse("{\n" +
                                "        $addFields: {\n" +
                                "            \"finishDate\": {\n" +
                                "                \"$add\": [\"$startTime\", {\n" +
                                "                        $multiply: [\"$durationMinutes\", 60000]\n" +
                                "            }]\n" +
                                "            }\n" +
                                "        }\n" +
                                "    }")),
                        a -> a.getMappedObject(Document.parse("{\n" +
                                "        $addFields: {\n" +
                                "            \"needFinish\": {\n" +
                                "                $gte: [new ISODate(), \"$finishDate\"]\n" +
                                "            }\n" +
                                "        }\n" +
                                "    }")),
                        a -> a.getMappedObject(Document.parse("{\n" +
                                "        $match: {\n" +
                                "            \"needFinish\": true\n" +
                                "        }\n" +
                                "    }"))/*,
                        a -> a.getMappedObject(Document.parse("{\n" +
                                "        $project: {\n" +
                                "            \"$_id\": 1\n" +
                                "        }\n" +
                                "    }"))*/
                ), Game.class, GameIdContainer.class
        ).getMappedResults().stream().map(GameIdContainer::getId).collect(Collectors.toList());
    }

    private static class GameIdContainer {
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }
}
