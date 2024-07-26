-- CREATE DATABASE  MeritMatch;

USE MeritMatch;
-- -- -- Recreate the table

-- CREATE TABLE users (
--     id INT AUTO_INCREMENT PRIMARY KEY,
--     username VARCHAR(255) UNIQUE NOT NULL ,
--     password VARCHAR(255) ,
--     age VARCHAR(255),
--     gender VARCHAR(10),
--     tasks JSON,
--     tasksTaken JSON,
--     tasksGiven JSON,
--     tasksPending JSON,
--     Comments JSON,
--     karmaPoints INT,
--     userOnline BOOLEAN
-- );


-- -- Check if the table is created successfully 
-- SHOW TABLES LIKE 'users';

-- Select all data from the users table

-- DROP TABLE users;

SELECT * FROM users;

