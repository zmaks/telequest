package com.mzheltoukhov.questpistols.repository;

import com.mzheltoukhov.questpistols.model.GameTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

public class GameTaskRepositoryImpl implements GameTaskRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public GameTask lockGameTask(String id) {
        return mongoTemplate.findAndModify(
                Query.query(Criteria.where("id").is(id).and("lock").is(false)),
                Update.update("lock", true),
                FindAndModifyOptions.options().returnNew(true),
                GameTask.class);
    }

    @Override
    public void unlockGameTask(String id) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(id)),
                Update.update("lock", false),
                GameTask.class);
    }
}
