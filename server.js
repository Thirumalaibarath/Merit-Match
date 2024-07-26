const express = require('express');
const mysql = require('mysql2');
const cors = require('cors');
const bodyParser = require('body-parser');
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');

const app = express();
const port = 3000;

// Middleware
app.use(cors());
app.use(bodyParser.json());

const connection = mysql.createConnection({
    host: 'localhost',
    user: 'root',
    password: 'Pushpa#23',
    database: 'MeritMatch'
});

connection.connect((err) => {
    if (err) {
        console.error('Error connecting to database:', err);
        return;
    }
    console.log('Connected to MySQL database');
});

const saltRounds = 10;

app.post('/register', (req, res) => {
    const { username, password, email, phone, address, age, gender } = req.body;

    // Basic validation
    if (!username || !password) {
        return res.status(400).json({ error: 'Username and password are required' });
    }

    // Password complexity validation
    const passwordPattern = /^(?=.*\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/;
    if (!passwordPattern.test(password)) {
        return res.status(400).json({ error: 'Password must be at least 8 characters long and include at least one uppercase letter, one lowercase letter, one digit, and one special character.' });
    }

    // Check if the user already exists
    const checkUserSql = 'SELECT * FROM users WHERE username = ?';
    connection.query(checkUserSql, [username], (err, results) => {
        if (err) {
            console.error('Error checking user:', err);
            return res.status(500).json({ error: 'Failed to check user' });
        }

        if (results.length > 0) {
            return res.status(409).json({ error: 'User already exists' });
        }

        // Hash the password
        bcrypt.hash(password, saltRounds, (err, hash) => {
            if (err) {
                console.error('Error hashing password:', err);
                return res.status(500).json({ error: 'Failed to register user' });
            }

            // Insert the new user with the hashed password
            const insertUserSql = 'INSERT INTO users (username, password, age, gender,KarmaPoints) VALUES (?, ?, ?, ?, 50)';
            connection.query(insertUserSql, [username, hash, age, gender], (err, results) => {
                if (err) {
                    console.error('Error inserting user:', err);
                    return res.status(500).json({ error: 'Failed to register user' });
                }
                console.log('User inserted successfully:', username);
                res.status(201).json({ message: 'User registered successfully', userId: results.insertId });
            });
        });
    });
});

// Fetch all users with names and karma points
app.get('/users', (req, res) => {
    const sql = 'SELECT username, karmaPoints FROM users';
    
    connection.query(sql, (err, results) => {
        if (err) {
            console.error('Error fetching users:', err);
            return res.status(500).json({ error: 'Failed to fetch users' });
        }

        // Send the results as a JSON response
        res.status(200).json(results);
    });
});

app.post('/login', (req, res) => {
    const { username, password } = req.body;

    if (!username || !password) {
        return res.status(400).json({ error: 'Username and password are required' });
    }

    const sql = 'SELECT * FROM users WHERE username = ?';
    connection.query(sql, [username], (err, results) => {
        if (err) {
            console.error('Error fetching user:', err);
            return res.status(500).json({ error: 'Failed to fetch user' });
        }

        if (results.length === 0) {
            return res.status(404).json({ error: 'Username does not exist' });
        }

        const user = results[0];

        bcrypt.compare(password, user.password, (err, isMatch) => {
            if (err) {
                console.error('Error comparing passwords:', err);
                return res.status(500).json({ error: 'Failed to authenticate' });
            }

            if (isMatch) {
                res.status(200).json({ 
                    message: 'Login successful', 
                    user: { 
                        username: user.username, 
                        age: user.age ,// or any other relevant user data
                        gender: user.gender,
                        karmaPoints: user.karmaPoints
                    } 
                });
            } else {
                res.status(401).json({ error: 'Incorrect password' });
            }
        });
    });


    // updating if the user is online 
    const getOnlineStatusOfUser = 'SELECT userOnline  FROM users WHERE username = ?';
    connection.query(getOnlineStatusOfUser, [username], (err, results) => {
        if (err) {
            console.error('Error fetching user:', err);
            return res.status(500).json({ error: 'Failed to fetch user' });
        }

        console.log(results[0].userOnline)

        const setOnlineStatusOfUser = 'UPDATE users SET userOnline = ? WHERE username = ?';

        connection.query(setOnlineStatusOfUser, [true,username], (err) => {
            if (err) {
                console.error('Error fetching user:', err);
                return res.status(500).json({ error: 'Failed to fetch user' });
            }
    
            console.log(results[0].userOnline)
    
    
        })

    


    })


});

// const JWT_SECRET = 'yep';

// // Middleware to authenticate JWT
// const authenticateJWT = (req, res, next) => {
//     const authHeader = req.headers.authorization;

//     if (authHeader) {
//         const token = authHeader.split(' ')[1];

//         jwt.verify(token, JWT_SECRET, (err, user) => {
//             if (err) {
//                 return res.sendStatus(403);
//             }

//             req.user = user;
//             next();
//         });
//     } else {
//         res.sendStatus(401);
//     }
// };

app.post('/add-task', (req, res) => {
    const { username, task,karmaPoints,dateTime } = req.body;

    if ( !task) {
        return res.status(400).json({ error: 'User ID and task are required' });
    }


    // Fetch existing tasks
    const getTasksSql = 'SELECT tasksPending FROM users WHERE username = ? ';
    connection.query(getTasksSql, [username], (err, taskResults) => {
        if (err) {
            console.error('Error fetching tasks:', err);
            return res.status(500).json({ error: 'Failed to fetch tasks' });
        }

        console.log(taskResults[0].tasks)
        
        
        let StructuredTaskforTask = {
            task : task,
            karmaPoints : karmaPoints,
            taskUploadTime: dateTime,
            taskTakenTimeL: "nil",
            taskStatus : "pending"
        }

        let StructuredTaskforPendingTask = {
            task : task,
            karmaPoints : karmaPoints,
            taskAvailableFrom : dateTime,
            taskTakenTime: "nil",
            taskStatus : "pending"
        }


        let tasksForTask = []
        let tasksForTaskPending = []


        if(taskResults[0].tasks != null)
        {
            for (let i = 0; i <taskResults[0].tasks.length; i++)
            {
                tasksForTask.push(taskResults[0].tasks[i]);
            }
        }

        if(taskResults[0].tasksPending != null)
        {
            for (let i = 0; i <taskResults[0].tasksPending.length; i++)
            {
                tasksForTaskPending.push(taskResults[0].tasksPending[i]);
            }
        }
        
        

        tasksForTask.push(StructuredTaskforTask)
        tasksForTaskPending.push(StructuredTaskforPendingTask)
        
        
        
        // Add the new task to the existing tasks
    
        // Update the tasks in the database
        const updateTasksSql = 'UPDATE users SET tasks = ? , tasksPending = ? WHERE username = ?';
        connection.query(updateTasksSql, [JSON.stringify(tasksForTask),JSON.stringify(tasksForTaskPending), username], (err) => {
            if (err) {
                console.error('Error updating tasks:', err);
                return res.status(500).json({ error: 'Failed to update tasks' });
            }

            res.status(201).json({ message: 'Task added successfully' });
        });

        



    });


});

// update the tasks and karma Points


app.post('/update-task-nature', (req, res) => {
    const { user,task,karmaPoints,taskNumber,dateAndTime } = req.body;

    if ( !task) {
        return res.status(400).json({ error: 'User ID and task are required' });
    }


    // Fetch existing tasks
    const getTasksSql = 'SELECT tasksPending FROM users WHERE username = ? ';
    connection.query(getTasksSql, [user], (err, taskResults) => {
        if (err) {
            console.error('Error fetching tasks:', err);
            return res.status(500).json({ error: 'Failed to fetch tasks' });
        }

        
        
        let StructuredUpdatedTask = {
            task : task,
            karmaPoints : karmaPoints,
            taskAvailableFrom : dateAndTime,
            taskTakenTime: "nil",
            taskStatus : "pending"
        }

        

        let tasks = []


        if(taskResults[0].tasksPending != null)
        {
            for (let i = 0; i <taskResults[0].tasksPending.length; i++)
            {
                tasks.push(taskResults[0].tasksPending[i]);
            }
        }
        
        

        tasks[taskNumber] = StructuredUpdatedTask
        
        
        
        // Add the new task to the existing tasks
    
        // Update the tasks in the database
        const updateTasksSql = 'UPDATE users SET tasks = ? , tasksPending = ? WHERE username = ?';
        connection.query(updateTasksSql, [JSON.stringify(tasks),JSON.stringify(tasks), user], (err) => {
            if (err) {
                console.error('Error updating tasks:', err);
                return res.status(500).json({ error: 'Failed to update tasks' });
            }

            res.status(201).json({ message: 'Task added successfully' });
        });

        
    });


});


// dropping a task 

app.post('/drop-task', (req, res) => {
    const { giver,user,task,karmaPoints,taskNumber,dateAndTime,comment} = req.body;

    var giverIndexNumber = 0

   
    if ( !task) {
        return res.status(400).json({ error: 'User ID and task are required' });
    }


    // Fetch existing tasks in taskTaken
    const getTasksSql = 'SELECT tasksTaken FROM users WHERE username = ? ';
    connection.query(getTasksSql, [user], (err, taskResults) => {
        if (err) {
            console.error('Error fetching tasks:', err);
            return res.status(500).json({ error: 'Failed to fetch tasks' });
        }

        let tasks = []


        for (let i = 0; i <taskResults[0].tasksTaken.length; i++)
            {
                if(i != taskNumber)
                {
                    tasks.push(taskResults[0].tasksTaken[i]);
                }
            
            }

            // update the tasktaken 

            const updateTasksTakenForTakerSql = 'UPDATE users SET tasksTaken = ?  WHERE username = ?';
            connection.query(updateTasksTakenForTakerSql, [JSON.stringify(tasks), user], (err) => {
                if (err) {
                    console.error('Error updating tasks:', err);
                    return res.status(500).json({ error: 'Failed to update tasks' });
                }
    
                res.status(201).json({ message: 'Task added successfully' });
            });

            // update the taskGiven for Giver 

            const updateTheTaskGivenForGiver = 'SELECT tasksGiven FROM users WHERE username = ? ';
            connection.query(updateTheTaskGivenForGiver, [giver], (err,taskResults) => {
                if (err) {
                    console.error('Error updating tasks:', err);
                    return res.status(500).json({ error: 'Failed to update tasks' });
                }

                if(taskResults[0].tasksGiven != null)
                {
                    for (let i = 0; i <taskResults[0].tasksGiven.length; i++)
                    {
                        if(taskResults[0].tasksGiven[i].task == task)
                        {
                            giverIndexNumber = i
                        }
                    
                    }
                }

                

            let tasks = []

            // updation 

            if(taskResults[0].tasksGiven != null)
            {
                for (let i = 0; i <taskResults[0].tasksGiven.length; i++)
                {
                    if(i != giverIndexNumber)
                    {
                        tasks.push(taskResults[0].tasksGiven[i]);
                    }
                
                }
            }

            

            const updateTasksGivenForGiverSql = 'UPDATE users SET tasksGiven = ?  WHERE username = ?';

            connection.query(updateTasksGivenForGiverSql, [JSON.stringify(tasks),giver], (err) => {
                if (err) {
                    console.error('Error updating tasks:', err);
                    return res.status(500).json({ error: 'Failed to update tasks' });
                }

                
            })

            
            });

            let structuredCommentForm = 
        {
            commentType : "Task Drop",
            from : user,
            to : giver ,
            comment : comment,
            DateAndTime : dateAndTime
        }


            // add Comments to  both of em 

            const updateCommentsForGiverSql = 'SELECT Comments FROM users WHERE username = ? ';

            connection.query(updateCommentsForGiverSql, [giver], (err,comments) => {
                if (err) {
                    console.error('Error updating tasks:', err);
                    return res.status(500).json({ error: 'Failed to update tasks' });
                }


                let commentsList = []

                if(comments[0].Comments != null)
        {
            for (let i = 0; i <comments[0].Comments.length; i++)
            {
                commentsList.push(comments[0].Comments[i]);
            }
        }

        

        commentsList.push(structuredCommentForm)

        const updateCommentsForGiver = 'UPDATE users SET Comments = ?  WHERE username = ?';

            connection.query(updateCommentsForGiver, [JSON.stringify(commentsList),giver], (err) => {
                if (err) {
                    console.error('Error updating tasks:', err);
                    return res.status(500).json({ error: 'Failed to update tasks' });
                }

                
            })


                  
            })


            // now for the user 

            const updateCommentsForDropperSql = 'SELECT Comments FROM users WHERE username = ? ';

            connection.query(updateCommentsForDropperSql, [user], (err,comments) => {
                if (err) {
                    console.error('Error updating tasks:', err);
                    return res.status(500).json({ error: 'Failed to update tasks' });
                }


                let commentsList = []

                if(comments[0].Comments != null)
        {
            for (let i = 0; i <comments[0].Comments.length; i++)
            {
                commentsList.push(comments[0].Comments[i]);
            }
        }

        

        commentsList.push(structuredCommentForm)


        const updateCommentsForDropper = 'UPDATE users SET Comments = ?  WHERE username = ?';

        connection.query(updateCommentsForDropper, [JSON.stringify(commentsList),user], (err) => {
            if (err) {
                console.error('Error updating tasks:', err);
                return res.status(500).json({ error: 'Failed to update tasks' });
            }

            
        })
                  
            })


            // now add the task in pending for giver 

            const getTaskPendingForGiver = 'SELECT tasksPending FROM users WHERE username = ? ';

            connection.query(getTaskPendingForGiver, [giver], (err,pendingTasks) => {
                if (err) {
                    console.error('Error updating tasks:', err);
                    return res.status(500).json({ error: 'Failed to update tasks' });
                }

                let taskPending = []

                if(pendingTasks[0].tasksPending != null)
        {
            for (let i = 0; i <pendingTasks[0].tasksPending.length; i++)
            {
                taskPending.push(pendingTasks[0].tasksPending[i]);
            }
        }

        let StructuredTask = {
            task : task,
            karmaPoints : karmaPoints,
            taskAvailableFrom: dateAndTime,
            taskTakenTime: "nil",
            taskStatus : "pending"
        }

        
        taskPending.push(StructuredTask)

        const updateTaskPendingForGiver = 'UPDATE users SET tasksPending = ?  WHERE username = ?';

        connection.query(updateTaskPendingForGiver, [JSON.stringify(taskPending),giver], (err) => {
            if (err) {
                console.error('Error updating tasks:', err);
                return res.status(500).json({ error: 'Failed to update tasks' });
            }

            
        })


            })

    });


});

// taskDoneSuccessfully 


// dropping a task 

app.post('/task-done', (req, res) => {
    const { giver,user,task,karmaPoints,taskNumber,dateAndTime,comment} = req.body;

    var giverIndexNumber = 0

   
    if ( !task) {
        return res.status(400).json({ error: 'User ID and task are required' });
    }


    // Fetch existing tasks in taskTaken
    const getTasksSql = 'SELECT tasksTaken FROM users WHERE username = ? ';
    connection.query(getTasksSql, [user], (err, taskResults) => {
        if (err) {
            console.error('Error fetching tasks:', err);
            return res.status(500).json({ error: 'Failed to fetch tasks' });
        }

        let tasks = []


        for (let i = 0; i <taskResults[0].tasksTaken.length; i++)
            {
                if(i != taskNumber)
                {
                    tasks.push(taskResults[0].tasksTaken[i]);
                }
            
            }

            // update the tasktaken 

            const updateTasksTakenForTakerSql = 'UPDATE users SET tasksTaken = ?  WHERE username = ?';
            connection.query(updateTasksTakenForTakerSql, [JSON.stringify(tasks), user], (err) => {
                if (err) {
                    console.error('Error updating tasks:', err);
                    return res.status(500).json({ error: 'Failed to update tasks' });
                }
    
                res.status(201).json({ message: 'Task added successfully' });
            });

            // update the taskGiven for Giver 

            const updateTheTaskGivenForGiver = 'SELECT tasksGiven FROM users WHERE username = ? ';
            connection.query(updateTheTaskGivenForGiver, [giver], (err,taskResults) => {
                if (err) {
                    console.error('Error updating tasks:', err);
                    return res.status(500).json({ error: 'Failed to update tasks' });
                }

                if(taskResults[0].tasksGiven != null)
                {
                    for (let i = 0; i <taskResults[0].tasksGiven.length; i++)
                    {
                        if(taskResults[0].tasksGiven[i].task == task)
                        {
                            giverIndexNumber = i
                        }
                    
                    }
                }

                

            let tasks = []

            // updation 

            if(taskResults[0].tasksGiven != null)
            {
                for (let i = 0; i <taskResults[0].tasksGiven.length; i++)
                {
                    if(i != giverIndexNumber)
                    {
                        tasks.push(taskResults[0].tasksGiven[i]);
                    }
                
                }
            }

            

            const updateTasksGivenForGiverSql = 'UPDATE users SET tasksGiven = ?  WHERE username = ?';

            connection.query(updateTasksGivenForGiverSql, [JSON.stringify(tasks),giver], (err) => {
                if (err) {
                    console.error('Error updating tasks:', err);
                    return res.status(500).json({ error: 'Failed to update tasks' });
                }

                
            })

            
            });

            let structuredCommentForm = 
        {
            commentType : "Task Done",
            from : user,
            to : giver ,
            comment : comment,
            DateAndTime : dateAndTime
        }

            // add Comments to  both of em 

            const updateCommentsForGiverSql = 'SELECT Comments FROM users WHERE username = ? ';

            connection.query(updateCommentsForGiverSql, [giver], (err,comments) => {
                if (err) {
                    console.error('Error updating tasks:', err);
                    return res.status(500).json({ error: 'Failed to update tasks' });
                }


                let commentsList = []

                if(comments[0].Comments != null)
        {
            for (let i = 0; i <comments[0].Comments.length; i++)
            {
                commentsList.push(comments[0].Comments[i]);
            }
        }

        

        commentsList.push(structuredCommentForm)

        const updateCommentsForGiver = 'UPDATE users SET Comments = ?  WHERE username = ?';

            connection.query(updateCommentsForGiver, [JSON.stringify(commentsList),giver], (err) => {
                if (err) {
                    console.error('Error updating tasks:', err);
                    return res.status(500).json({ error: 'Failed to update tasks' });
                }

                
            })


                  
            })


            // now for the user 

            const updateCommentsForDropperSql = 'SELECT Comments FROM users WHERE username = ? ';

            connection.query(updateCommentsForDropperSql, [user], (err,comments) => {
                if (err) {
                    console.error('Error updating tasks:', err);
                    return res.status(500).json({ error: 'Failed to update tasks' });
                }


                let commentsList = []

                if(comments[0].Comments != null)
        {
            for (let i = 0; i <comments[0].Comments.length; i++)
            {
                commentsList.push(comments[0].Comments[i]);
            }
        }

        

        commentsList.push(structuredCommentForm)


        const updateCommentsForDropper = 'UPDATE users SET Comments = ?  WHERE username = ?';

        connection.query(updateCommentsForDropper, [JSON.stringify(commentsList),user], (err) => {
            if (err) {
                console.error('Error updating tasks:', err);
                return res.status(500).json({ error: 'Failed to update tasks' });
            }

            
        })
                  
            })


            // now update  the karmaPoints in pending for giver 

            const fetchKarmaPointsForGiver = 'SELECT karmaPoints FROM users WHERE username = ? ';

            connection.query(fetchKarmaPointsForGiver, [giver], (err,giverKarmaPoints) => {
                if (err) {
                    console.error('Error updating tasks:', err);
                    return res.status(500).json({ error: 'Failed to update tasks' });
                }

                var giverPoints = giverKarmaPoints[0].karmaPoints - parseInt(karmaPoints)

                // console.log(giverPoints)
                // console.log(typeof giverPoints)
                // console.log(typeof giverKarmaPoints[0].karmaPoints)


                const updateKarmaPointsForGiver = 'UPDATE users SET karmaPoints = ?  WHERE username = ?';

                connection.query(updateKarmaPointsForGiver, [giverPoints,giver], (err) => {
                    if (err) {
                        console.error('Error updating tasks:', err);
                        return res.status(500).json({ error: 'Failed to update tasks' });
                    }
                })


            })

            //

            const fetchKarmaPointsForUser = 'SELECT karmaPoints FROM users WHERE username = ? ';

            connection.query(fetchKarmaPointsForUser, [user], (err,giverKarmaPoints) => {
                if (err) {
                    console.error('Error updating tasks:', err);
                    return res.status(500).json({ error: 'Failed to update tasks' });
                }

                var userPoints = giverKarmaPoints[0].karmaPoints + parseInt(karmaPoints)

            
                const updateKarmaPointsForUser = 'UPDATE users SET karmaPoints = ?  WHERE username = ?';

                connection.query(updateKarmaPointsForUser, [userPoints,user], (err) => {
                    if (err) {
                        console.error('Error updating tasks:', err);
                        return res.status(500).json({ error: 'Failed to update tasks' });
                    }
                })


            })
            

    });


});








// // adding online tasks 
// app.post('/task-done', (req, res) => {
//     const {currentusername,username,taskNumber,karmaPoints} = req.body;

    
//     // Fetch - giver  tasks
//     const getKarmaPointSqlforGiver = 'SELECT karmaPoints FROM users WHERE username = ? ';

//     connection.query(getKarmaPointSqlforGiver, [currentusername], (err, giverPoints) => {
//         if (err) {
//             console.error('Error fetching tasks:', err);
//             return res.status(500).json({ error: 'Failed to fetch tasks' });
//         }

    
//         var changedPoints = giverPoints[0].karmaPoints - karmaPoints

//         // console.log(typeof giverPoints[0].karmaPoints )
//         // console.log(giverPoints[0].karmaPoints - 10 )

//         const updateKarmaPointsSql = 'UPDATE users SET karmaPoints = ? WHERE username = ?';

//         connection.query(updateKarmaPointsSql,[changedPoints,currentusername], (err) => {
//             if (err) {
//                 console.error('Error fetching tasks:', err);
//                 return res.status(500).json({ error: 'Failed to fetch tasks' });
//             }
    
//         });

//     });

//     // doer Karma Points 

//     const getKarmaPointSqlforDoer = 'SELECT karmaPoints FROM users WHERE username = ? ';

//     connection.query(getKarmaPointSqlforDoer, [username], (err, doerPoints) => {
//         if (err) {
//             console.error('Error fetching tasks:', err);
//             return res.status(500).json({ error: 'Failed to fetch tasks' });
//         }

    
//         var addPoints = doerPoints[0].karmaPoints + karmaPoints

//         const updateKarmaPointsSql = 'UPDATE users SET karmaPoints = ? WHERE username = ?';

//         connection.query(updateKarmaPointsSql,[addPoints,username], (err) => {
//             if (err) {
//                 console.error('Error fetching tasks:', err);
//                 return res.status(500).json({ error: 'Failed to fetch tasks' });
//             }
    
//         });

//     });

//     // deletion of the task 

//     const gettaskSqlforGiver = 'SELECT tasks FROM users WHERE username = ? ';

//     connection.query(gettaskSqlforGiver, [currentusername], (err, givertasks) => {
//         if (err) {
//             console.error('Error fetching tasks:', err);
//             return res.status(500).json({ error: 'Failed to fetch tasks' });
//         }


//         let tasks = []

//         for (let i = 0; i <givertasks[0].tasks.length; i++)
//             {
//                 if(i != taskNumber)
//                 {
//                     tasks.push(givertasks[0].tasks[i]);
//                 }
            
//             }
        

//         console.log(tasks)

//         const updatetaskSql = 'UPDATE users SET tasks = ? WHERE username = ?';

//         connection.query(updatetaskSql,[JSON.stringify(tasks),currentusername], (err) => {
//             if (err) {
//                 console.error('Error fetching tasks:', err);
//                 return res.status(500).json({ error: 'Failed to fetch tasks' });
//             }
    
//         });

//     });

// });

// // adding online tasks 
// app.post('/tasks-taking', (req, res) => {
//     const {giver,taker,taskNumber,karmaPoints,timeAndDate} = req.body;


//     // fetch the task from the giver
//     const fetch_task_from_giver = 'SELECT tasksPending FROM users WHERE username = ? ';

//     connection.query(fetch_task_from_giver,[giver], (err,giverTask) => {
//         if (err) {
//             console.error('Error fetching tasks:', err);
//             return res.status(500).json({ error: 'Failed to fetch tasks' });
//         }

//         // console.log(giverTask[0].tasksPending)

//         let StructuredTaskTaken = {
//             taskGiverName:giver,
//             task : giverTask[0].tasksPending[taskNumber].task,
//             karmaPoints : giverTask[0].tasksPending[taskNumber].karmaPoints,
//             taskTakenTime: timeAndDate,
//         }

//             // add task taken for taker
//     const add_task_taken_for_taker = 'SELECT tasksTaken FROM users WHERE username = ? ';

//     connection.query(add_task_taken_for_taker, [taker], (err, takerTasks) => {
//         if (err) {
//             console.error('Error fetching tasks:', err);
//             return res.status(500).json({ error: 'Failed to fetch tasks' });
//         }

//         let tasks = []


//         if(takerTasks[0].tasksTaken != null)
//         {
//             for (let i = 0; i <takerTasks[0].tasksTaken.length; i++)
//             {
//                 tasks.push(takerTasks[0].tasksTaken[i]);
//             }
//         }
        
        
//         tasks.push(StructuredTaskTaken)

//          // Update the tasks in the database for the taker
//          const updateTakenTasksforTaker = 'UPDATE users SET tasksTaken = ? WHERE username = ?';
//          connection.query(updateTakenTasksforTaker, [JSON.stringify(tasks), taker], (err) => {
//              if (err) {
//                  console.error('Error updating tasks:', err);
//                  return res.status(500).json({ error: 'Failed to update tasks' });
//              }
 
//              res.status(201).json({ message: 'Task added successfully' });
//          });

         
//     });

//     let tasksPending = []

//     for (let i = 0; i <giverTask[0].tasksPending.length; i++)
//             {
//                 if(i != taskNumber)
//                 {
//                     tasksPending.push(giverTask[0].tasksPending[i]);
//                 }
            
//             }

//         const updatependingTasksforGiver = 'UPDATE users SET tasksPending = ? WHERE username = ?';
//         connection.query(updatependingTasksforGiver, [JSON.stringify(tasksPending), giver], (err) => {
//             if (err) {
//                 console.error('Error updating tasks:', err);
//                 return res.status(500).json({ error: 'Failed to update tasks' });
//             }

//             res.status(201).json({ message: 'Task added successfully' });
//         });


    
//     });

       
// });





app.post('/delete-user-task', (req, res) => {
    const { username,taskNumber } = req.body;

   
    // Fetch the task from the giver
    const fetch_taskPending_from_giver = 'SELECT tasksPending FROM users WHERE username = ? ';

    connection.query(fetch_taskPending_from_giver, [username], (err, taskPending) => {
        if (err) {
            console.error('Error fetching tasks:', err);
            return res.status(500).json({ error: 'Failed to fetch tasks' });
        }

        // if (!giverTask[0].tasksPending || giverTask[0].tasksPending.length <= taskNumber) {
        //     return res.status(400).json({ error: 'Task not found' });
        // }

        let taskPendingList = []
        
       
        if(taskPending[0].taskPending != null)
        {
            for (let i = 0; i <taskPending[0].taskPending.length; i++)
            {
                if(i != parseInt(taskNumber))
                {
                    taskPendingList.push(taskPending[0].taskPending[i]);
                }
            }
        }

        // update 

    const update_taskPending_for_user = 'UPDATE users SET tasksPending = ? WHERE username = ?'

    connection.query(update_taskPending_for_user, [JSON.stringify(taskPendingList),username], (err) => {
        if (err) {
            console.error('Error fetching tasks:', err);
            return res.status(500).json({ error: 'Failed to fetch tasks' });
        }

    });
    
    });

    


});







app.post('/tasks-taking', (req, res) => {
    const { giver, taker, taskNumber, karmaPoints, timeAndDate } = req.body;

    // Fetch the task from the giver
    const fetch_task_from_giver = 'SELECT tasksPending FROM users WHERE username = ? ';

    connection.query(fetch_task_from_giver, [giver], (err, giverTask) => {
        if (err) {
            console.error('Error fetching tasks:', err);
            return res.status(500).json({ error: 'Failed to fetch tasks' });
        }

        if (!giverTask[0].tasksPending || giverTask[0].tasksPending.length <= taskNumber) {
            return res.status(400).json({ error: 'Task not found' });
        }

        let StructuredTaskTaken = {
            taskGiverName: giver,
            task: giverTask[0].tasksPending[taskNumber].task,
            karmaPoints: giverTask[0].tasksPending[taskNumber].karmaPoints,
            taskTakenTime: timeAndDate,
        };



        // update in taskGiven

    const fetch_taskGiven_for_giver = 'SELECT tasksGiven FROM users WHERE username = ? '


    connection.query(fetch_taskGiven_for_giver, [giver], (err, giverTaskUpdate) => {
        if (err) {
            console.error('Error fetching tasks:', err);
            return res.status(500).json({ error: 'Failed to fetch tasks' });
        }

        let StructuredTaskGiven = {
            taskTakerName: taker,
            task: giverTask[0].tasksPending[taskNumber].task,
            karmaPoints: giverTask[0].tasksPending[taskNumber].karmaPoints,
            taskGivenTime: timeAndDate,
        };

        let tasksGiven = [];

        if (giverTaskUpdate[0].tasksGiven != null) {
            for (let i = 0; i <giverTaskUpdate[0].tasksGiven.length; i++)
        {
            tasksGiven.push(giverTaskUpdate[0].tasksGiven[i]);
        }
        }

        tasksGiven.push(StructuredTaskGiven);

        const update_taskGiven_for_giver = 'UPDATE users SET tasksGiven = ? WHERE username = ?'
 
        connection.query(update_taskGiven_for_giver, [JSON.stringify(tasksGiven), giver], (err) => {
            if (err) {
                console.error('Error updating tasks:', err);
                return res.status(500).json({ error: 'Failed to update tasks' });
            }
        });

    });

        // Add task taken for taker
        const add_task_taken_for_taker = 'SELECT tasksTaken FROM users WHERE username = ? ';

        connection.query(add_task_taken_for_taker, [taker], (err, takerTasks) => {
            if (err) {
                console.error('Error fetching tasks:', err);
                return res.status(500).json({ error: 'Failed to fetch tasks' });
            }

            let tasks = [];

            if (takerTasks[0].tasksTaken != null) {
                for (let i = 0; i <takerTasks[0].tasksTaken.length; i++)
            {
                tasks.push(takerTasks[0].tasksTaken[i]);
            }
            }

            tasks.push(StructuredTaskTaken);

            // Update the tasks in the database for the taker
            const updateTakenTasksforTaker = 'UPDATE users SET tasksTaken = ? WHERE username = ?';
            connection.query(updateTakenTasksforTaker, [JSON.stringify(tasks), taker], (err) => {
                if (err) {
                    console.error('Error updating tasks:', err);
                    return res.status(500).json({ error: 'Failed to update tasks' });
                }

                let tasksPending = [];

                for (let i = 0; i < giverTask[0].tasksPending.length; i++) {
                    if (i != taskNumber) {
                        tasksPending.push(giverTask[0].tasksPending[i]);
                    }
                }

                const updatependingTasksforGiver = 'UPDATE users SET tasksPending = ? WHERE username = ?';
                connection.query(updatependingTasksforGiver, [JSON.stringify(tasksPending), giver], (err) => {
                    if (err) {
                        console.error('Error updating tasks:', err);
                        return res.status(500).json({ error: 'Failed to update tasks' });
                    }

                    res.status(201).json({ message: 'Task added successfully' });
                });
            });
        });
    });

    


});



// Define the endpoint to get all user Taken tasks
app.get('/get-all-usertakentasks', (req, res) => {
    const getAllUserTakenTasksSql = 'SELECT username, tasksTaken FROM users';
    
    connection.query(getAllUserTakenTasksSql, (err, results) => {
        if (err) {
            console.error('Error fetching tasks:', err);
            return res.status(500).json({ error: 'Failed to fetch tasks' });
        }

    
        res.json(results);
    
    });

    
});




// Define the endpoint to get all shared tasks
app.get('/get-all-shared-tasks', (req, res) => {
    const getAllTasksSql = 'SELECT username, tasksGiven FROM users';
    
    connection.query(getAllTasksSql, (err, results) => {
        if (err) {
            console.error('Error fetching tasks:', err);
            return res.status(500).json({ error: 'Failed to fetch tasks' });
        }

    
        res.json(results);
    });

    
});





// Define the endpoint to get all tasks
app.get('/get-all-tasks', (req, res) => {
    const getAllTasksSql = 'SELECT username, tasksPending FROM users WHERE tasksPending IS NOT NULL';
    
    connection.query(getAllTasksSql, (err, results) => {
        if (err) {
            console.error('Error fetching tasks:', err);
            return res.status(500).json({ error: 'Failed to fetch tasks' });
        }

    
        res.json(results);
    
    });

    
});

app.post('/user-offline', (req, res) => {

    const { username } = req.body;

    const setOnlineStatusOfUser = 'UPDATE users SET userOnline = ? WHERE username = ?';

    connection.query(setOnlineStatusOfUser, [false,username], (err) => {
    if (err) {
        console.error('Error fetching user:', err);
        return res.status(500).json({ error: 'Failed to fetch user' });
    }

    // console.log(results[0].userOnline)


})

});

// Define the endpoint to get all tasks
app.get('/login-stat', (req, res) => {
    const getAllTasksSql = 'SELECT username, userOnline FROM users WHERE userOnline IS NOT NULL';
    
    connection.query(getAllTasksSql, (err, results) => {
        if (err) {
            console.error('Error fetching tasks:', err);
            return res.status(500).json({ error: 'Failed to fetch tasks' });
        }

    
        res.json(results);
    
    });

    
});


// the endpoint to get all Comments
app.get('/get-all-usercomments', (req, res) => {
    const getAllTasksSql = 'SELECT username, Comments FROM users WHERE userOnline IS NOT NULL';
    
    connection.query(getAllTasksSql, (err, results) => {
        if (err) {
            console.error('Error fetching tasks:', err);
            return res.status(500).json({ error: 'Failed to fetch tasks' });
        }

        res.json(results);
    
    });

    
});




// Start the server
app.listen(port, () => {
    console.log(`Server running at http://localhost:${port}`);
});
