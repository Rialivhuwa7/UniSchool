-- Parents and learners can ask for access for learners the school hasn't enrolled in UniSchool yet.
-- Their details wait here until the school office approves the request, which creates the learner.
create table access_request_new_learners (
    request_id     bigint       not null references access_requests (id),
    student_number varchar(255),
    first_name     varchar(255) not null,
    last_name      varchar(255) not null,
    grade          varchar(255) not null,
    campus         varchar(255) not null
);
