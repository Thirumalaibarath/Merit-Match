package com.example.loginpage

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.lifecycle.lifecycleScope
import com.example.loginpage.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

var mainState = mutableStateListOf<Boolean>(false,false,false,false,false)
var takeUpTask by mutableStateOf(false)

var selectedTaskNumber by mutableIntStateOf(0)


var editMyTaskState  = mutableStateListOf<Boolean>(false,false,false,false,false)
var myWorkState  = mutableStateListOf<Boolean>(false,false,false)
var myShareState  = mutableStateListOf<Boolean>(false,false)




class MainActivity2 : ComponentActivity() {
    private val serverUrl = "http://10.2.9.126:3000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val username = intent.getStringExtra("username")
        val age = intent.getStringExtra("age")
        val gender = intent.getStringExtra("gender")
        val karmaPoints = intent.getStringExtra("karma points")

        setContent {
            MainScreen(username, age, gender,karmaPoints,this)
            Log.d("name",username.toString())
        }
    }

    private fun uploadTask(task: String, karmaPoints: String,username :String, serverUrl: String) {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("token", null)
        val userId = sharedPreferences.getString("userId", null)

        val currentDateTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val formattedDateTime = currentDateTime.format(formatter)


        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("$serverUrl/add-task")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8")
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.doOutput = true

                val jsonParam = JSONObject().apply {
                    put("task", task)
                    put("karmaPoints", karmaPoints)
                    put("username" , username)
                    put("dateTime", formattedDateTime)
                }

                val os = OutputStreamWriter(conn.outputStream)
                os.write(jsonParam.toString())
                os.flush()
                os.close()

                val responseCode = conn.responseCode

                if (responseCode == HttpURLConnection.HTTP_CREATED) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    withContext(Dispatchers.Main) {
                        val jsonObject = JSONObject(response.toString())
                        val message = jsonObject.getString("message")
                        Toast.makeText(this@MainActivity2, message, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity2, "Error: $responseCode", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity2, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun retrieveUserTasks(serverUrl: String,user:String, onTasksFetched: (List<Map<String, Any>>) -> Unit) {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("token", null)


        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("$serverUrl/get-all-tasks")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("Authorization", "Bearer $token")

                val responseCode = conn.responseCode
                Log.d("API_RESPONSE", "Response Code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    // Log the response
                    val responseString = response.toString()
                    Log.d("API_RESPONSE", "Raw Response: $responseString")

                    withContext(Dispatchers.Main) {
                        try {
                            // Parse JSON response
                            val jsonArray = JSONArray(responseString)
                            Log.d("API_RESPONSE", "Number of items in JSON array: ${jsonArray.length()}")

                            // Convert JSONArray to a list of task objects
                            val allTasks = mutableListOf<Map<String, Any>>()
                            for (i in 0 until jsonArray.length()) {
                                val userObject = jsonArray.getJSONObject(i)
                                val userList = userObject.getString("username")

                                // Retrieve tasks only for the onlineUsername
                                if (userList == user) {
                                    val tasksArray = userObject.getJSONArray("tasksPending")
                                    for (j in 0 until tasksArray.length()) {
                                        val taskObject = tasksArray.getJSONObject(j)
                                        val task = taskObject.getString("task")
                                        val karmaPoints = taskObject.getString("karmaPoints")
                                        val taskStatus = taskObject.getString("taskStatus")
                                        allTasks.add(mapOf("username" to userList, "task" to task, "karmaPoints" to karmaPoints, "taskStatus" to taskStatus))
                                    }
                                }
                            }

                            Log.d("API_RESPONSE", "All tasks: $allTasks")

                            // Call the callback to update the state
                            onTasksFetched(allTasks)
                        } catch (e: JSONException) {
                            Log.e("API_RESPONSE", "JSON Parsing Error: ${e.message}")
                            Toast.makeText(this@MainActivity2, "Error parsing tasks", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity2, "Error: $responseCode", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity2, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun retrieveTasks(serverUrl: String, user:String, onTasksFetched: (List<Map<String, Any>>) -> Unit) {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("token", null)


        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("$serverUrl/get-all-tasks")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("Authorization", "Bearer $token")


                val responseCode = conn.responseCode
                Log.d("API_RESPONSE", "Response Code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    // Log the response
                    val responseString = response.toString()
                    Log.d("API_RESPONSE", "Raw Response: $responseString")

                    withContext(Dispatchers.Main) {
                        try {
                            // Parse JSON response
                            val jsonArray = JSONArray(responseString)
                            Log.d("API_RESPONSE", "Number of items in JSON array: ${jsonArray.length()}")

                            // Convert JSONArray to a list of task objects
                            val allTasks = mutableListOf<Map<String, Any>>()
                            for (i in 0 until jsonArray.length()) {
                                val userObject = jsonArray.getJSONObject(i)
                                val userList = userObject.getString("username")

//                                 Skip tasks for the onlineUsername
                                if (userList == user) {
                                    continue
                                }

                                val tasksArray = userObject.getJSONArray("tasksPending")
                                if(tasksArray != null)
                                {
                                    for (j in 0 until tasksArray.length()) {
                                        val taskObject = tasksArray.getJSONObject(j)
                                        val task = taskObject.getString("task")
                                        val karmaPoints = taskObject.getString("karmaPoints")
                                        val taskStatus = taskObject.getString("taskStatus")
                                        allTasks.add(mapOf("taskIndex" to j ,"username" to userList, "task" to task, "karmaPoints" to karmaPoints, "taskStatus" to taskStatus))
                                    }
                                }

                            }

                            Log.d("API_RESPONSE", "All tasks: $allTasks")

                            // Call the callback to update the state
                            onTasksFetched(allTasks)
                        } catch (e: JSONException) {
                            Log.e("API_RESPONSE", "JSON Parsing Error: ${e.message}")
                            Toast.makeText(this@MainActivity2, "Error parsing tasks", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity2, "Error: $responseCode", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity2, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun tasksDone(serverUrl: String,dropper:String,giver:String,task:String,taskNumber:String,karmaPoints: String,comment:String) {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("token", null)

        val currentDateTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val formattedDateTime = currentDateTime.format(formatter)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("$serverUrl/task-done")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8")
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.doOutput = true


                val jsonParam = JSONObject().apply {
                    put("user", dropper)
                    put("giver", giver)
                    put("taskNumber" , taskNumber)
                    put("karmaPoints",karmaPoints)
                    put("task",task)
                    put("dateAndTime",formattedDateTime)
                    put("comment",comment)
                }



                val os = OutputStreamWriter(conn.outputStream)
                os.write(jsonParam.toString())
                os.flush()
                os.close()

                val responseCode = conn.responseCode

                if (responseCode == HttpURLConnection.HTTP_CREATED) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    withContext(Dispatchers.Main) {
                        val jsonObject = JSONObject(response.toString())
                        val message = jsonObject.getString("message")
                        Toast.makeText(this@MainActivity2, message, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity2, "Error: $responseCode", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity2, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }





    private fun retrieveSharedTasks(serverUrl: String, user:String, onTasksFetched: (List<Map<String, Any>>) -> Unit) {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("token", null)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("$serverUrl/get-all-shared-tasks")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("Authorization", "Bearer $token")


                val responseCode = conn.responseCode
                Log.d("API_RESPONSE", "Response Code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    // Log the response
                    val responseString = response.toString()
                    Log.d("API_RESPONSE", "Raw Response: $responseString")

                    withContext(Dispatchers.Main) {
                        try {
                            // Parse JSON response
                            val jsonArray = JSONArray(responseString)
                            Log.d("API_RESPONSE", "Number of items in JSON array: ${jsonArray.length()}")

                            // Convert JSONArray to a list of task objects
                            val allTasks = mutableListOf<Map<String, Any>>()
                            for (i in 0 until jsonArray.length()) {
                                val userObject = jsonArray.getJSONObject(i)
                                val userList = userObject.getString("username")

                                if (userList == user) {
                                    val tasksArray = userObject.getJSONArray("tasksGiven")
                                    for (j in 0 until tasksArray.length()) {
                                        val taskObject = tasksArray.getJSONObject(j)
                                        val task = taskObject.getString("task")
                                        val taskTaker = taskObject.getString("taskTakerName")
                                        val karmaPoints = taskObject.getString("karmaPoints")
                                        val taskGivenTime = taskObject.getString("taskGivenTime")


                                        allTasks.add(mapOf("username" to taskTaker, "task" to task, "karmaPoints" to karmaPoints,"tasksGivenTime" to taskGivenTime))
                                    }
                                }
                            }

                            Log.d("API_RESPONSE", "All tasks: $allTasks")

                            // Call the callback to update the state
                            onTasksFetched(allTasks)
                        } catch (e: JSONException) {
                            Log.e("API_RESPONSE", "JSON Parsing Error: ${e.message}")
                            Toast.makeText(this@MainActivity2, "Error parsing tasks", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity2, "Error: $responseCode", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity2, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun retrieveUserTakenTasks(serverUrl: String, user:String, onTasksFetched: (List<Map<String, Any>>) -> Unit) {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("token", null)


        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("$serverUrl/get-all-usertakentasks")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("Authorization", "Bearer $token")


                val responseCode = conn.responseCode
                Log.d("API_RESPONSE", "Response Code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    // Log the response
                    val responseString = response.toString()
                    Log.d("API_RESPONSE", "Raw Response: $responseString")

                    withContext(Dispatchers.Main) {
                        try {
                            // Parse JSON response
                            val jsonArray = JSONArray(responseString)
                            Log.d("API_RESPONSE", "Number of items in JSON array: ${jsonArray.length()}")

                            // Convert JSONArray to a list of task objects
                            val allTasks = mutableListOf<Map<String, Any>>()
                            for (i in 0 until jsonArray.length()) {
                                val userObject = jsonArray.getJSONObject(i)
                                val userList = userObject.getString("username")


                                if(userList == user )
                                {
                                    Log.d("index",i.toString())
                                    val tasksArray = userObject.getJSONArray("tasksTaken")

                                    if(tasksArray != null)
                                    {
                                        for (j in 0 until tasksArray.length()) {
                                            val taskObject = tasksArray.getJSONObject(j)
                                            val taskNumber = j
                                            val taskGiverName = taskObject.getString("taskGiverName")
                                            val task = taskObject.getString("task")
                                            val karmaPoints = taskObject.getString("karmaPoints")
                                            allTasks.add(mapOf("username" to taskGiverName, "taskNumber" to taskNumber ,"task" to task, "karmaPoints" to karmaPoints))
                                        }
                                    }

                                }


                            }

                            Log.d("API_RESPONSE", "All tasks: $allTasks")

                            // Call the callback to update the state
                            onTasksFetched(allTasks)
                        } catch (e: JSONException) {
                            Log.e("API_RESPONSE", "JSON Parsing Error: ${e.message}")
                            Toast.makeText(this@MainActivity2, "Error parsing tasks", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity2, "Error: $responseCode", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity2, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // task adding
    private fun tasksUpdater(serverUrl: String,giver:String,taker:String,taskNumber:String,karmaPoints: String) {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("token", null)

        val currentDateTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val formattedDateTime = currentDateTime.format(formatter)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("$serverUrl/tasks-taking")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8")
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.doOutput = true


                val jsonParam = JSONObject().apply {
                    put("giver", giver)
                    put("taker", taker)
                    put("taskNumber" , taskNumber)
                    put("karmaPoints",karmaPoints)
                    put("timeAndDate",formattedDateTime)
                }

                val os = OutputStreamWriter(conn.outputStream)
                os.write(jsonParam.toString())
                os.flush()
                os.close()

                val responseCode = conn.responseCode

                if (responseCode == HttpURLConnection.HTTP_CREATED) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    withContext(Dispatchers.Main) {
                        val jsonObject = JSONObject(response.toString())
                        val message = jsonObject.getString("message")
                        Toast.makeText(this@MainActivity2, message, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity2, "Error: $responseCode", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity2, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // task adding
    private fun updateTaskNature(serverUrl: String,user:String,task:String,taskNumber:String,karmaPoints: String) {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("token", null)

        val currentDateTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val formattedDateTime = currentDateTime.format(formatter)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("$serverUrl/update-task-nature")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8")
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.doOutput = true


                val jsonParam = JSONObject().apply {
                    put("user", user)
                    put("taskNumber" , taskNumber)
                    put("karmaPoints",karmaPoints)
                    put("task",task)
                    put("dateAndTime",formattedDateTime)
                }

                val os = OutputStreamWriter(conn.outputStream)
                os.write(jsonParam.toString())
                os.flush()
                os.close()

                val responseCode = conn.responseCode

                if (responseCode == HttpURLConnection.HTTP_CREATED) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    withContext(Dispatchers.Main) {
                        val jsonObject = JSONObject(response.toString())
                        val message = jsonObject.getString("message")
                        Toast.makeText(this@MainActivity2, message, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity2, "Error: $responseCode", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity2, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    // task adding
    private fun dropTask(serverUrl: String,dropper:String,giver:String,task:String,taskNumber:String,karmaPoints: String,comment:String) {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("token", null)

        val currentDateTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val formattedDateTime = currentDateTime.format(formatter)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("$serverUrl/drop-task")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8")
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.doOutput = true


                val jsonParam = JSONObject().apply {
                    put("user", dropper)
                    put("giver", giver)
                    put("taskNumber" , taskNumber)
                    put("karmaPoints",karmaPoints)
                    put("task",task)
                    put("dateAndTime",formattedDateTime)
                    put("comment",comment)
                }

                val os = OutputStreamWriter(conn.outputStream)
                os.write(jsonParam.toString())
                os.flush()
                os.close()

                val responseCode = conn.responseCode

                if (responseCode == HttpURLConnection.HTTP_CREATED) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    withContext(Dispatchers.Main) {
                        val jsonObject = JSONObject(response.toString())
                        val message = jsonObject.getString("message")
                        Toast.makeText(this@MainActivity2, message, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity2, "Error: $responseCode", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity2, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun logout(serverUrl: String,username:String) {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("token", null)

        val currentDateTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val formattedDateTime = currentDateTime.format(formatter)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("$serverUrl/user-offline")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8")
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.doOutput = true


                val jsonParam = JSONObject().apply {
                    put("username", username)
                }

                val os = OutputStreamWriter(conn.outputStream)
                os.write(jsonParam.toString())
                os.flush()
                os.close()

                val responseCode = conn.responseCode

                if (responseCode == HttpURLConnection.HTTP_CREATED) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    withContext(Dispatchers.Main) {
                        val jsonObject = JSONObject(response.toString())
                        val message = jsonObject.getString("message")
                        Toast.makeText(this@MainActivity2, message, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity2, "Error: $responseCode", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity2, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun userTaskDeleter(serverUrl: String,username:String,taskNumber:String) {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("token", null)

        val currentDateTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val formattedDateTime = currentDateTime.format(formatter)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("$serverUrl/delete-user-task")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8")
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.doOutput = true


                val jsonParam = JSONObject().apply {
                    put("username", username)
                    put("taskNumber", taskNumber)
                }

                val os = OutputStreamWriter(conn.outputStream)
                os.write(jsonParam.toString())
                os.flush()
                os.close()

                val responseCode = conn.responseCode

                if (responseCode == HttpURLConnection.HTTP_CREATED) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    withContext(Dispatchers.Main) {
                        val jsonObject = JSONObject(response.toString())
                        val message = jsonObject.getString("message")
                        Toast.makeText(this@MainActivity2, message, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity2, "Error: $responseCode", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity2, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun retrieveUserComments(serverUrl: String, user:String, onCommentsFetched: (List<Map<String, Any>>) -> Unit) {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("token", null)


        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("$serverUrl/get-all-usercomments")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("Authorization", "Bearer $token")


                val responseCode = conn.responseCode
                Log.d("API_RESPONSE", "Response Code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    // Log the response
                    val responseString = response.toString()
                    Log.d("API_RESPONSE", "Raw Response: $responseString")

                    withContext(Dispatchers.Main) {
                        try {
                            // Parse JSON response
                            val jsonArray = JSONArray(responseString)
                            Log.d("API_RESPONSE", "Number of items in JSON array: ${jsonArray.length()}")

                            // Convert JSONArray to a list of task objects
                            val allTasks = mutableListOf<Map<String, Any>>()
                            for (i in 0 until jsonArray.length()) {
                                val userObject = jsonArray.getJSONObject(i)
                                val userList = userObject.getString("username")


                                if(userList == user )
                                {
                                    val tasksArray = userObject.getJSONArray("Comments")

                                    for (j in 0 until tasksArray.length()) {
                                        val taskObject = tasksArray.getJSONObject(j)
                                        val commentTo = taskObject.getString("to")
                                        val commentFrom = taskObject.getString("from")
                                        val dateAndTime = taskObject.getString("DateAndTime")
                                        val comments  = taskObject.getString("comment")
                                        val commentType  = taskObject.getString("commentType")
                                        allTasks.add(mapOf("commentTo" to  commentTo,"commentFrom" to commentFrom, "dateAndTime" to dateAndTime,"comments" to comments,"commentType" to commentType))
                                    }
                                }


                            }

                            Log.d("API_RESPONSE", "All tasks: $allTasks")

                            // Call the callback to update the state
                            onCommentsFetched(allTasks)
                        } catch (e: JSONException) {
                            Log.e("API_RESPONSE", "JSON Parsing Error: ${e.message}")
                            Toast.makeText(this@MainActivity2, "Error parsing tasks", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity2, "Error: $responseCode", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity2, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }



    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainScreen(
        username: String?,
        age: String?,
        gender: String?,
        karmaPoints:String?,
        context: Context
    ) {

        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp.dp
        val screenHeight = configuration.screenHeightDp.dp

        val verticalSpacing = (screenHeight - 700.dp )/3


        val backgroundColor = Color(0xFFADBADA)
        Surface(modifier = Modifier.fillMaxSize(),
            color = backgroundColor) {
            Column(
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            )
            {
                val space = (screenWidth - 40.dp)/4

                val innerColor = Color(0xFF8697C3)
                val fontColor = Color(0xFF3d52a1)
                val taskColor = Color(0xFFadbada)
                val headerColor = Color(0xFFeee8f6)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .padding(20.dp,0.dp)
                            .background(color = innerColor, shape = RoundedCornerShape(20.dp)),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    )
                    {
                        Icon(painter = painterResource(id = R.drawable.user), contentDescription = "user",tint = if(mainState[0]) fontColor else Color.Black , modifier = Modifier.clickable {
                            mainState[0] = !mainState[0]
                            mainState[1] = false
                            mainState[2] = false
                            mainState[3] = false
                            mainState[4] = false
                        })

                        Icon(painter = painterResource(id = R.drawable.task), contentDescription = "user",tint =if(mainState[1]) fontColor else Color.Black,modifier = Modifier.clickable {
                            mainState[1] = !mainState[1]
                            mainState[0] = false
                            mainState[2] = false
                            mainState[3] = false
                            mainState[4] = false
                        })

                        Icon(painter = painterResource(id = R.drawable.history), contentDescription = "user",tint =if(mainState[2]) fontColor else Color.Black,modifier = Modifier.clickable {
                            mainState[2] = !mainState[2]
                            mainState[1] = false
                            mainState[0] = false
                            mainState[3] = false
                            mainState[4] = false
                        })

                        Icon(painter = painterResource(id = R.drawable.manageuser), contentDescription = "user",tint =if(mainState[3]) fontColor else Color.Black,modifier = Modifier.clickable {
                            mainState[3] = !mainState[3]
                            mainState[1] = false
                            mainState[0] = false
                            mainState[2] = false
                            mainState[4] = false
                        })

                        Icon(painter = painterResource(id = R.drawable.logout), contentDescription = "user",tint =if(mainState[4]) fontColor else Color.Black,modifier = Modifier.clickable {
                            mainState[4] = !mainState[4]
                            mainState[1] = false
                            mainState[0] = false
                            mainState[2] = false
                            mainState[3] = false
                        })

                    }




                Box(
                    modifier = Modifier
                        .height(650.dp)
                        .fillMaxWidth()
                        .padding(20.dp,0.dp)
                        .background(color = innerColor, shape = RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                )
                {
                    if(mainState[0])
                    {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally

                        )
                        {
                            Text("Username: $username", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("Date Of Birth: $age",fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("Gender: $gender",fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("Karma Points: $karmaPoints",fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(70.dp),
                            )
                            {
                                Box(
                                    modifier = Modifier.clickable {
                                        val intent = Intent(context, MainActivity::class.java)
                                        context.startActivity(intent)
                                    }
                                )
                                {
                                    Icon(painter = painterResource(id = R.drawable.manageuser), contentDescription = "user",tint = Color.Black)
                                }

                                Box(
                                    modifier = Modifier.clickable {

                                        if (username != null) {
                                            logout(serverUrl = serverUrl,username = username)
                                        }

                                        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                                        val editor = sharedPreferences.edit()
                                        editor.putBoolean("isLoggedIn", false)
                                        editor.remove("username")
                                        editor.remove("password")
                                        editor.apply()

                                        val intent = Intent(context, MainActivity::class.java)
                                        context.startActivity(intent)
                                        finish()
                                    }
                                )
                                {
                                    Icon(painter = painterResource(id = R.drawable.logout), contentDescription = "user",tint = Color.Black)
                                }
                            }

                        }
                    }
                    else if(mainState[1])
                    {
                        Column(
                            Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        )
                        {
                            val space = (screenWidth - 60.dp)/10
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .padding(10.dp)
                                    .border(width = 2.dp, color = Color.Blue),
                                horizontalArrangement = Arrangement.spacedBy(space,Alignment.CenterHorizontally),
                            )
                            {
                                Icon(painter = painterResource(id = R.drawable.add), contentDescription = "user",tint = if(state[0]) fontColor else Color.Black  , modifier = Modifier.clickable {
                                    state[0] = true
                                    state[1] = false
                                    state[2] = false
                                    state[3] = false
                                    state[4] = false
                                })
                                Icon(painter = painterResource(id = R.drawable.storage), contentDescription = "user",tint = if(state[1]) fontColor else Color.Black,
                                    modifier = Modifier.clickable {
                                        state[0] = false
                                        state[1] = true
                                        state[2] = false
                                        state[3] = false
                                        state[4] = false
                                    })
                                Icon(painter = painterResource(id = R.drawable.build), contentDescription = "user",tint = if(state[2]) fontColor else Color.Black,modifier = Modifier.clickable {
                                    state[0] = false
                                    state[1] = false
                                    state[2] = true
                                    state[3] = false
                                    state[4] = false
                                })
                                Icon(painter = painterResource(id = R.drawable.group), contentDescription = "user",tint = if(state[3]) fontColor else Color.Black,
                                    modifier = Modifier.clickable {
                                        state[0] = false
                                        state[1] = false
                                        state[2] = false
                                        state[3] =  true
                                        state[4] = false
                                    })
                                Icon(painter = painterResource(id = R.drawable.work), contentDescription = "user",tint = if(state[4]) fontColor else Color.Black,
                                    modifier = Modifier.clickable {
                                        state[0] = false
                                        state[1] = false
                                        state[2] = false
                                        state[3] = false
                                        state[4] = true
                                    })
                            }

                            var tasks by remember { mutableStateOf("") }
                            var karmaPoints by remember { mutableStateOf("") }
                            var taskDescription by remember { mutableStateOf("") }
                            var displayKarmaPoints by remember { mutableStateOf("") }

                            if(state[0])
                            {
                                Column(
                                    modifier = Modifier.padding(10.dp)
                                        .height(580.dp)
                                        .border(width = 2.dp, color = Color.Blue),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement =  Arrangement.spacedBy(20.dp)
                                )
                                {
                                    TextField(
                                        value = tasks,
                                        onValueChange = { tasks = it },
                                        label = { Text(text = "tasks",color = Color.Black) },
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        textStyle = TextStyle.Default.copy(
                                            fontSize = 20.sp,
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        colors = TextFieldDefaults.textFieldColors(
                                            unfocusedLabelColor = Color.Transparent,
                                            focusedLabelColor = Color.Transparent,
                                            cursorColor = Color.Black,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent,
                                            disabledIndicatorColor = Color.Transparent,
                                            unfocusedLeadingIconColor = taskColor,
                                            containerColor = taskColor,
                                            focusedTrailingIconColor = Color.White,
                                            unfocusedTrailingIconColor = Color.White,
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        trailingIcon = {
                                            Icon(Icons.Default.BorderColor, contentDescription = "Select Date" ,tint = Color.Black)
                                        }
                                    )

                                    TextField(
                                        value = karmaPoints,
                                        onValueChange = { karmaPoints = it },
                                        label = { Text(text = "Karma Points",color = Color.Black) },
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        textStyle = TextStyle.Default.copy(
                                            fontSize = 20.sp,
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        colors = TextFieldDefaults.textFieldColors(
                                            unfocusedLabelColor = Color.Transparent,
                                            focusedLabelColor = Color.Transparent,
                                            cursorColor = Color.Black,
                                            focusedLeadingIconColor = Color.White,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent,
                                            disabledIndicatorColor = Color.Transparent,
                                            unfocusedLeadingIconColor = taskColor,
                                            containerColor = taskColor,
                                            focusedTrailingIconColor = Color.White,
                                            unfocusedTrailingIconColor = Color.White,
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        trailingIcon = {
                                            Icon(Icons.Default.Add, contentDescription = "Select Date" ,tint = Color.Black)
                                        }
                                    )

                                    Button(
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = fontColor ),
                                        shape = RoundedCornerShape(10.dp),
                                        onClick = {
                                            if (username != null) {
                                                uploadTask(tasks,karmaPoints,username,serverUrl)
                                            }
                                        })
                                    {
                                        Text(text = "Upload Task",color = Color.Black)
                                    }
                                }

                                }
                            else if(state[1])
                            {
                                val tasks = remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

                                var tasksheet by remember { mutableStateOf("") }
                                var karmaPoints by remember { mutableStateOf("") }

                                var taskIndex by remember { mutableIntStateOf(-1) }

                                // Call retrieveTasks when the screen is loaded or refreshed
                                LaunchedEffect(Unit) {
                                    while (true) {
                                        if (username != null) {
                                            retrieveUserTasks(user = username, serverUrl = serverUrl) { fetchedTasks ->
                                                tasks.value = fetchedTasks
                                            }
                                        }
                                        delay(300)
                                    }
                                }
//                                val index = 0 // Change this to the index you want to access
//                                val taskAtIndex = tasks.value.getOrNull(index)
//                                val usernameAtIndex = taskAtIndex?.get("username")?.toString()
//
//                                if (usernameAtIndex != null) {
//                                    Text(text = "Username at index $index: $usernameAtIndex")
//                                } else {
//                                    Text(text = "No task at index $index")
//                                }

                                // Display tasks in a table



                                Column(
                                    modifier = Modifier.padding(10.dp)
                                        .height(580.dp)
                                        .border(width = 2.dp, color = Color.Blue),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                )
                                {
                                    Row(
                                        modifier = Modifier
                                            .padding(10.dp)
                                            .fillMaxWidth()
                                            .height(40.dp)
                                            .background(color = backgroundColor,shape = RoundedCornerShape(12.dp))
                                            ,
                                        horizontalArrangement = Arrangement.spacedBy(30.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier.height(40.dp)
                                                .width(30.dp)
                                                .border(width = 2.dp, color = Color.Blue),
                                            contentAlignment = Alignment.Center
                                        )
                                        {
                                            Text(text = "s.no", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        }

                                        Box(
                                            modifier = Modifier.height(40.dp)
                                                .width(140.dp)
                                                .border(width = 2.dp, color = Color.Blue),
                                            contentAlignment = Alignment.CenterStart
                                        )
                                        {
                                            Text(text = "task", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        }

                                        Box(
                                            modifier = Modifier.height(40.dp)
                                                .width(80.dp)
                                                .border(width = 2.dp, color = Color.Blue),
                                            contentAlignment = Alignment.Center
                                        )
                                        {
                                            Text(text = "task-status", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        }


                                    }

                                        LazyColumn(
                                            modifier = Modifier.height(280.dp)
                                                .border(width = 2.dp, color = Color.Red),
                                            verticalArrangement = Arrangement.spacedBy(5.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            itemsIndexed(tasks.value) { index, task ->

                                                val backgroundColor = if (taskIndex == index) fontColor else taskColor

                                                Row(
                                                    modifier = Modifier
                                                        .padding(10.dp)
                                                        .fillMaxWidth()
                                                        .height(40.dp)
                                                        .background(color = backgroundColor,shape = RoundedCornerShape(12.dp))
                                                        .clickable {
                                                            tasksheet = task["task"].toString()
                                                            karmaPoints = task["karmaPoints"].toString()
                                                            taskIndex = index
                                                        },
                                                    horizontalArrangement = Arrangement.spacedBy(30.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {

                                                    Box(
                                                        modifier = Modifier.height(40.dp)
                                                            .width(30.dp)
                                                            .border(width = 2.dp, color = Color.Blue),
                                                        contentAlignment = Alignment.Center
                                                    )
                                                    {
                                                        Text(text = (index+1).toString()+".", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                    }

                                                    Box(
                                                        modifier = Modifier.height(40.dp)
                                                            .width(140.dp)
                                                            .border(width = 2.dp, color = Color.Blue),
                                                        contentAlignment = Alignment.CenterStart
                                                    )
                                                    {
                                                        Text(text = task["task"].toString(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                    }


                                                    Box(
                                                        modifier = Modifier.height(40.dp)
                                                            .width(80.dp)
                                                            .border(width = 2.dp, color = Color.Blue),
                                                        contentAlignment = Alignment.Center
                                                    )
                                                    {
                                                        Icon(painter = painterResource(id = R.drawable.pending), contentDescription = "user",tint = Color.Black, modifier = Modifier.size(30.dp)

                                                            .border(width = 2.dp, color = Color.Blue))
                                                    }

                                                }
                                            }
                                        }


                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(30.dp)
                                            .border(width = 2.dp, color = Color.Blue),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    )
                                    {
                                        Icon(painter = painterResource(id = R.drawable.info), contentDescription = "user",tint = if(editMyTaskState[0]) fontColor else Color.Black, modifier = Modifier.clickable {
                                            editMyTaskState[0] = !editMyTaskState[0]
                                            editMyTaskState[1] = false
                                            editMyTaskState[2] = false
                                        })
                                        Icon(painter = painterResource(id = R.drawable.edit), contentDescription = "user",tint = if(editMyTaskState[1]) fontColor else Color.Black,modifier = Modifier.clickable {
                                            editMyTaskState[0] = false
                                            editMyTaskState[1] =!editMyTaskState[1]
                                            editMyTaskState[2] = false
                                        })
                                        Icon(painter = painterResource(id = R.drawable.delete), contentDescription = "user",tint = if(editMyTaskState[2]) fontColor else Color.Black,modifier = Modifier.clickable {
                                            editMyTaskState[0] = false
                                            editMyTaskState[1] = false
                                            editMyTaskState[2] = !editMyTaskState[2]
                                        })
                                    }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(230.dp)
                                            .border(width = 2.dp, color = Color.Blue),
                                        verticalArrangement = Arrangement.SpaceEvenly
                                    )
                                    {
                                        if(editMyTaskState[1])
                                        {

                                                TextField(
                                                    value = tasksheet,
                                                    onValueChange = { tasksheet = it },
                                                    label = { Text(text = "tasks",color = Color.Black) },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(100.dp)
                                                    ,
                                                    textStyle = TextStyle.Default.copy(
                                                        fontSize = 20.sp,
                                                        color = Color.Black,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    colors = TextFieldDefaults.textFieldColors(
                                                        unfocusedLabelColor = Color.Transparent,
                                                        focusedLabelColor = Color.Transparent,
                                                        cursorColor = Color.Black,
                                                        focusedLeadingIconColor = Color.White,
                                                        focusedIndicatorColor = Color.Transparent,
                                                        unfocusedIndicatorColor = Color.Transparent,
                                                        disabledIndicatorColor = Color.Transparent,
                                                        unfocusedLeadingIconColor = Color.White,
                                                        containerColor = Color.White,
                                                        focusedTrailingIconColor = Color.White,
                                                        unfocusedTrailingIconColor = Color.White,
                                                    ),
                                                    shape = RoundedCornerShape(10.dp),
                                                    trailingIcon = {
                                                        Icon(Icons.Default.BorderColor, contentDescription = "Select Date" ,tint = Color.Black)
                                                    }
                                                )

                                            val fieldSize = ((screenWidth - 60.dp)*1)/2
                                            val space = (screenWidth - 60.dp) - fieldSize - 30.dp

                                            Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(space/2,Alignment.Start),
                                                    verticalAlignment = Alignment.CenterVertically
                                                )
                                                {
                                                    TextField(
                                                        value = karmaPoints,
                                                        onValueChange = { karmaPoints = it },
                                                        label = { Text(text = "Karma Points",color = Color.Black) },
                                                        modifier = Modifier.width(fieldSize)
                                                            .height(50.dp)

                                                        ,
                                                        textStyle = TextStyle.Default.copy(
                                                            fontSize = 20.sp,
                                                            color = Color.Black,
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        colors = TextFieldDefaults.textFieldColors(
                                                            unfocusedLabelColor = Color.Transparent,
                                                            focusedLabelColor = Color.Transparent,
                                                            cursorColor = Color.Black,
                                                            focusedLeadingIconColor = Color.White,
                                                            focusedIndicatorColor = Color.Transparent,
                                                            unfocusedIndicatorColor = Color.Transparent,
                                                            disabledIndicatorColor = Color.Transparent,
                                                            unfocusedLeadingIconColor = Color.White,
                                                            containerColor = Color.White,
                                                            focusedTrailingIconColor = Color.White,
                                                            unfocusedTrailingIconColor = Color.White,
                                                        ),
                                                        shape = RoundedCornerShape(10.dp),
                                                        trailingIcon = {
                                                            Icon(Icons.Default.Add, contentDescription = "Select Date" ,tint = Color.Black)
                                                        }
                                                    )

                                                    Icon(painter = painterResource(id = R.drawable.upload), contentDescription = "user",tint =  Color.Black,modifier = Modifier
                                                        .size(30.dp)
                                                        .clickable {

                                                        if (username != null) {
                                                            updateTaskNature(serverUrl = serverUrl,user = username ,task = tasksheet,taskNumber = taskIndex.toString(), karmaPoints = karmaPoints )
                                                        }
                                                    })
                                                }

                                        }


                                        if(editMyTaskState[0])
                                        {
                                            Box(
                                                modifier = Modifier.fillMaxWidth()
                                                    .background(color = fontColor, shape = RoundedCornerShape(12.dp))
                                                    .height(100.dp),
                                                contentAlignment = Alignment.CenterStart

                                            )
                                            {
                                                Text(text="TASK: $tasksheet" , modifier = Modifier.padding(20.dp,0.dp), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                            }

                                            val fieldSize = ((screenWidth - 60.dp)*4)/10
                                            val space = (screenWidth - 60.dp) - 2*fieldSize

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(space,Alignment.Start),
                                                verticalAlignment = Alignment.CenterVertically
                                            )
                                            {
                                                Box(
                                                    modifier = Modifier.width(fieldSize)
                                                        .background(color = fontColor, shape = RoundedCornerShape(12.dp))
                                                        .height(50.dp),
                                                    contentAlignment = Alignment.CenterStart

                                                )
                                                {
                                                    Text(text="KARMA POINTS: $karmaPoints" , modifier = Modifier.padding(15.dp,0.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }

                                                Box(
                                                    modifier = Modifier.width(fieldSize)
                                                        .background(color = fontColor, shape = RoundedCornerShape(12.dp))
                                                        .height(50.dp),
                                                    contentAlignment = Alignment.CenterStart,


                                                )
                                                {
                                                    Text(text="UPLOAD TIME: $taskDescription" , modifier = Modifier.padding(15.dp,0.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                        }

                                        if(editMyTaskState[2])
                                        {

                                            Box(
                                                modifier = Modifier.fillMaxWidth()
                                                    .background(color = fontColor, shape = RoundedCornerShape(12.dp))
                                                    .height(100.dp),
                                                contentAlignment = Alignment.CenterStart

                                            )
                                            {
                                                Text(text="TASK: $tasksheet" , modifier = Modifier.padding(20.dp,0.dp), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                            }

                                            val fieldSize = ((screenWidth - 60.dp)*1)/2
                                            val space = (screenWidth - 60.dp) - fieldSize - 30.dp

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(space/2,Alignment.Start),
                                                verticalAlignment = Alignment.CenterVertically
                                            )
                                            {
                                                Box(
                                                    modifier = Modifier.width(fieldSize)
                                                        .background(color = fontColor, shape = RoundedCornerShape(12.dp))
                                                        .height(50.dp),
                                                    contentAlignment = Alignment.CenterStart

                                                )
                                                {
                                                    Text(text="KARMA POINTS: $karmaPoints" , modifier = Modifier.padding(15.dp,0.dp), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                                }

                                                Icon(painter = painterResource(id = R.drawable.delete), contentDescription = "user",tint =  Color.Black,modifier = Modifier
                                                    .size(30.dp)
                                                    .clickable {
                                                        if (username != null) {
                                                            userTaskDeleter(serverUrl = serverUrl,username = username,taskNumber = taskIndex.toString())
                                                        }
//                                                        Toast.makeText(this@MainActivity2, taskIndex.toString(), Toast.LENGTH_SHORT).show()
                                                    })


                                            }
                                        }


                                    }


                            }

                            }
                            else if(state[2])
                            {

                                val tasks = remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

                                // Call retrieveTasks when the screen is loaded or refreshed
                                LaunchedEffect(Unit) {
                                    while (true) {
                                        if (username != null) {
                                            retrieveUserTakenTasks(user = username, serverUrl = serverUrl) { fetchedTasks ->
                                                tasks.value = fetchedTasks
                                            }
                                        }
                                        delay(300)
                                    }
                                }
//                                val index = 0 // Change this to the index you want to access
//                                val taskAtIndex = tasks.value.getOrNull(index)
//                                val usernameAtIndex = taskAtIndex?.get("username")?.toString()
//
//                                if (usernameAtIndex != null) {
//                                    Text(text = "Username at index $index: $usernameAtIndex")
//                                } else {
//                                    Text(text = "No task at index $index")
//                                }

                                // Display tasks in a table

                                var selectedIndex by remember { mutableIntStateOf(-1) }
                                var giver by remember { mutableStateOf("") }
                                var taskName by remember { mutableStateOf("") }
                                var taskNumber by remember { mutableStateOf("") }
                                var karmaPoints by remember { mutableStateOf("") }

                                Column(
                                    modifier = Modifier.padding(10.dp)
                                        .height(580.dp)
                                        .border(width = 2.dp, color = Color.Blue),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                )
                                {
                                    Row(
                                        modifier = Modifier
                                            .padding(10.dp)
                                            .fillMaxWidth()
                                            .height(40.dp)
                                            .background(color =  headerColor,shape = RoundedCornerShape(12.dp))
                                        ,
                                        horizontalArrangement = Arrangement.spacedBy(30.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier.height(40.dp)
                                                .width(30.dp)
                                                .border(width = 2.dp, color = Color.Blue),
                                            contentAlignment = Alignment.Center
                                        )
                                        {
                                            Text(text = "s.no", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        }

                                        Box(
                                            modifier = Modifier.height(40.dp)
                                                .width(140.dp)
                                                .border(width = 2.dp, color = Color.Blue),
                                            contentAlignment = Alignment.CenterStart
                                        )
                                        {
                                            Text(text = "task", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        }

                                        Box(
                                            modifier = Modifier.height(40.dp)
                                                .width(80.dp)
                                                .border(width = 2.dp, color = Color.Blue),
                                            contentAlignment = Alignment.Center
                                        )
                                        {
                                            Text(text = "task-status", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        }


                                    }

                                        LazyColumn(
                                            modifier = Modifier.height(260.dp)
                                                .border(width = 2.dp, color = Color.Red),
                                            verticalArrangement = Arrangement.spacedBy(5.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            itemsIndexed(tasks.value) { index, task ->

                                                val backgroundColor = if (selectedIndex == index) fontColor else taskColor

                                                Row(
                                                    modifier = Modifier
                                                        .padding(10.dp)
                                                        .fillMaxWidth()
                                                        .height(40.dp)
                                                        .background(color = backgroundColor,shape = RoundedCornerShape(12.dp))
                                                        .clickable {

                                                            selectedIndex = index
                                                            // Handle row click
                                                            giver = task["username"].toString()
                                                            taskName = task["task"].toString()
                                                            karmaPoints = task["karmaPoints"].toString()
                                                            taskNumber = task["taskNumber"].toString()
                                                            taskDescription = task["task"].toString()
                                                            displayKarmaPoints = task["karmaPoints"].toString()

                                                            Log.d("message","dropper: $username giver: $giver task: $taskName karmaPoints: $karmaPoints ")

                                                        },
                                                    horizontalArrangement = Arrangement.spacedBy(30.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier.height(40.dp)
                                                            .width(30.dp)
                                                            .border(width = 2.dp, color = Color.Blue),
                                                        contentAlignment = Alignment.Center
                                                    )
                                                    {
                                                        Text(text = (index+1).toString()+".", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                    }

                                                    Box(
                                                        modifier = Modifier.height(40.dp)
                                                            .width(140.dp)
                                                            .border(width = 2.dp, color = Color.Blue),
                                                        contentAlignment = Alignment.CenterStart
                                                    )
                                                    {
                                                        Text(text = task["task"].toString(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                    }

                                                    Box(
                                                        modifier = Modifier.height(40.dp)
                                                            .width(80.dp)
                                                            .border(width = 2.dp, color = Color.Blue),
                                                        contentAlignment = Alignment.Center
                                                    )
                                                    {
                                                        Icon(painter = painterResource(id = R.drawable.bolt), contentDescription = "user",tint =  Color.Black,modifier = Modifier.size(30.dp))
                                                    }

                                                }
                                            }
                                        }


                                    var comment by remember { mutableStateOf("") }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(30.dp)
                                            .border(width = 2.dp, color = Color.Blue),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    )
                                    {
                                        Icon(painter = painterResource(id = R.drawable.info), contentDescription = "user",tint = if(myWorkState[0]) fontColor else Color.Black,modifier = Modifier.clickable {
                                            myWorkState[0] = !myWorkState[0]
                                            myWorkState[1] = false
                                            myWorkState[2] = false

                                        })
                                        Icon(painter = painterResource(id = R.drawable.remove), contentDescription = "user",tint = if(myWorkState[1]) fontColor else Color.Black,modifier = Modifier.clickable {
                                            myWorkState[0] = false
                                            myWorkState[1] = !myWorkState[1]
                                            myWorkState[2] = false
                                        })
                                        Icon(painter = painterResource(id = R.drawable.doneall), contentDescription = "user",tint = if(myWorkState[2]) fontColor else Color.Black,modifier = Modifier.clickable {
                                            myWorkState[0] = false
                                            myWorkState[1] = false
                                            myWorkState[2] = !myWorkState[2]
                                        })
                                    }

                                    Column(
                                        modifier = Modifier.fillMaxWidth()
                                            .border(width = 2.dp, color = Color.Blue)
                                            .height(230.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceAround
                                    )
                                    {


                                        if(myWorkState[0])
                                        {
                                            if(selectedIndex != -1)
                                            {

                                                Box(
                                                    modifier = Modifier.fillMaxWidth()
                                                        .background(color = fontColor, shape = RoundedCornerShape(12.dp))
                                                        .height(50.dp),
                                                    contentAlignment = Alignment.CenterStart

                                                )
                                                {
                                                    Text(text="TASK GIVER: $giver" , modifier = Modifier.padding(20.dp,0.dp), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Box(
                                                    modifier = Modifier.fillMaxWidth()
                                                        .background(color = fontColor, shape = RoundedCornerShape(12.dp))
                                                        .height(110.dp),
                                                    contentAlignment = Alignment.CenterStart

                                                )
                                                {
                                                    Text(text="TASK: $taskName" , modifier = Modifier.padding(20.dp,0.dp), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                                }
                                                val fieldSize = ((screenWidth - 60.dp)*4)/10
                                                val space = (screenWidth - 60.dp) - 2*fieldSize

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(space,Alignment.Start),
                                                    verticalAlignment = Alignment.CenterVertically
                                                )
                                                {
                                                    Box(
                                                        modifier = Modifier.width(fieldSize)
                                                            .background(color = fontColor, shape = RoundedCornerShape(12.dp))
                                                            .height(50.dp),
                                                        contentAlignment = Alignment.CenterStart

                                                    )
                                                    {
                                                        Text(text="KARMA POINTS: $karmaPoints" , modifier = Modifier.padding(15.dp,0.dp), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                                    }

                                                    Box(
                                                        modifier = Modifier.width(fieldSize)
                                                            .background(color = fontColor, shape = RoundedCornerShape(12.dp))
                                                            .height(50.dp),
                                                        contentAlignment = Alignment.CenterStart

                                                    )
                                                    {
                                                        Text(text="TIMING: --" , modifier = Modifier.padding(15.dp,0.dp), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }

                                        if(myWorkState[1])
                                        {

                                            val maxWidth = (screenWidth - 60.dp)
                                            val fieldSize = ((screenWidth - 60.dp)*8)/10
                                            val space = (screenWidth - 60.dp) - fieldSize - 30.dp

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(space/2,Alignment.Start),
                                                verticalAlignment = Alignment.CenterVertically
                                            )
                                            {
                                                TextField(
                                                    value = comment,
                                                    onValueChange = { comment = it },
                                                    label = { Text(text = "Reason For Dropping",color = Color.Black) },
                                                    modifier = Modifier
                                                        .width(fieldSize)
                                                        .height(180.dp)
                                                    ,
                                                    textStyle = TextStyle.Default.copy(
                                                        fontSize = 20.sp,
                                                        color = Color.Black,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    colors = TextFieldDefaults.textFieldColors(
                                                        unfocusedLabelColor = Color.Transparent,
                                                        focusedLabelColor = Color.Transparent,
                                                        cursorColor = Color.Black,
                                                        focusedLeadingIconColor = Color.White,
                                                        focusedIndicatorColor = Color.Transparent,
                                                        unfocusedIndicatorColor = Color.Transparent,
                                                        disabledIndicatorColor = Color.Transparent,
                                                        unfocusedLeadingIconColor = Color.White,
                                                        containerColor = taskColor,
                                                        focusedTrailingIconColor = Color.White,
                                                        unfocusedTrailingIconColor = Color.White,
                                                    ),
                                                    shape = RoundedCornerShape(10.dp),
                                                )

                                                Icon(painter = painterResource(id = R.drawable.upload), contentDescription = "user",tint =  Color.Black,modifier = Modifier
                                                    .size(30.dp)
                                                    .clickable {
                                                        if (username != null && comment.isNotEmpty()) {
                                                            dropTask(serverUrl = serverUrl,dropper = username ,giver = giver ,task = taskName ,taskNumber = taskNumber,karmaPoints = karmaPoints,comment = comment)
                                                        }


                                                    })



                                            }
                                        }


                                        if(myWorkState[2])
                                        {
                                            val maxWidth = (screenWidth - 60.dp)
                                            val fieldSize = ((screenWidth - 60.dp)*8)/10
                                            val space = (screenWidth - 60.dp) - fieldSize - 30.dp

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(space/2,Alignment.Start),
                                                verticalAlignment = Alignment.CenterVertically
                                            )
                                            {
                                                TextField(
                                                    value = comment,
                                                    onValueChange = { comment = it },
                                                    label = { Text(text = "how was the task?",color = Color.Black) },
                                                    modifier = Modifier
                                                        .width(fieldSize)
                                                        .height(180.dp)
                                                    ,
                                                    textStyle = TextStyle.Default.copy(
                                                        fontSize = 20.sp,
                                                        color = Color.Black,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    colors = TextFieldDefaults.textFieldColors(
                                                        unfocusedLabelColor = Color.Transparent,
                                                        focusedLabelColor = Color.Transparent,
                                                        cursorColor = Color.Black,
                                                        focusedLeadingIconColor = Color.White,
                                                        focusedIndicatorColor = Color.Transparent,
                                                        unfocusedIndicatorColor = Color.Transparent,
                                                        disabledIndicatorColor = Color.Transparent,
                                                        unfocusedLeadingIconColor = Color.White,
                                                        containerColor = taskColor,
                                                        focusedTrailingIconColor = Color.White,
                                                        unfocusedTrailingIconColor = Color.White,
                                                    ),
                                                    shape = RoundedCornerShape(10.dp),
                                                )

                                                Icon(painter = painterResource(id = R.drawable.upload), contentDescription = "user",tint =  Color.Black,modifier = Modifier
                                                    .size(30.dp)
                                                    .clickable {

                                                    if (username != null && comment.isNotEmpty() ) {
                                                        tasksDone(
                                                            serverUrl = serverUrl,
                                                            dropper = username,
                                                            giver = giver,
                                                            task = taskName,
                                                            taskNumber = taskNumber,
                                                            karmaPoints = karmaPoints,
                                                            comment = comment
                                                        )
                                                    }
                                                })



                                            }
                                        }




                                    }




                            }






                        }
                            else if (state[3])
                            {
                                val tasks = remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }


                                // Call retrieveTasks when the screen is loaded or refreshed
                                LaunchedEffect(Unit) {
                                    while (true) {
                                        if (username != null) {
                                            retrieveSharedTasks(user = username, serverUrl = serverUrl) { fetchedTasks ->
                                                tasks.value = fetchedTasks
                                            }
                                        }
                                        delay(300)
                                    }
                                }


                                var selectedIndex by remember { mutableIntStateOf(-1) }
                                var giver by remember { mutableStateOf("") }
                                var taskName by remember { mutableStateOf("") }
                                var karmaPoints by remember { mutableStateOf("") }
                                var time by remember { mutableStateOf("") }

                                Column(
                                    modifier = Modifier.padding(10.dp)
                                        .height(580.dp)
                                        .border(width = 2.dp, color = Color.Blue),
                                    horizontalAlignment = Alignment.CenterHorizontally,)
                                {
                                    Row(
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .background(color =  headerColor,shape = RoundedCornerShape(12.dp))
                                    ,
                                    horizontalArrangement = Arrangement.spacedBy(30.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.height(40.dp)
                                            .width(30.dp)
                                            .border(width = 2.dp, color = Color.Blue),
                                        contentAlignment = Alignment.Center
                                    )
                                    {
                                        Text(text = "s.no", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }

                                    Box(
                                        modifier = Modifier.height(40.dp)
                                            .width(140.dp)
                                            .border(width = 2.dp, color = Color.Blue),
                                        contentAlignment = Alignment.CenterStart
                                    )
                                    {
                                        Text(text = "task", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }

                                    Box(
                                        modifier = Modifier.height(40.dp)
                                            .width(80.dp)
                                            .border(width = 2.dp, color = Color.Blue),
                                        contentAlignment = Alignment.Center
                                    )
                                    {
                                        Text(text = "task-status", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }


                                }

                                    LazyColumn(
                                        modifier = Modifier.height(260.dp)
                                            .border(width = 2.dp, color = Color.Red),
                                        verticalArrangement = Arrangement.spacedBy(5.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        itemsIndexed(tasks.value) { index, task ->

                                            val backgroundColor = if (selectedIndex == index) fontColor else taskColor

                                            Row(
                                                modifier = Modifier
                                                    .padding(10.dp)
                                                    .fillMaxWidth()
                                                    .height(40.dp)
                                                    .background(color = backgroundColor,shape = RoundedCornerShape(12.dp))
                                                    .clickable {

                                                        selectedIndex = index
                                                        // Handle row click
                                                        giver = task["username"].toString()
                                                        karmaPoints = task["karmaPoints"].toString()
                                                        taskName = task["task"].toString()
                                                        time = task["tasksGivenTime"].toString()

                                                        Toast.makeText(
                                                            this@MainActivity2,
                                                            task["taskIndex"].toString(),
                                                            Toast.LENGTH_SHORT
                                                        ).show()

                                                    }
                                                ,
                                                horizontalArrangement = Arrangement.spacedBy(30.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier.height(40.dp)
                                                        .width(30.dp)
                                                        .border(width = 2.dp, color = Color.Blue),
                                                    contentAlignment = Alignment.Center
                                                )
                                                {
                                                    Text(text = (index+1).toString()+".", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                }

                                                Box(
                                                    modifier = Modifier.height(40.dp)
                                                        .width(140.dp)
                                                        .border(width = 2.dp, color = Color.Blue),
                                                    contentAlignment = Alignment.CenterStart
                                                )
                                                {
                                                    Text(text = task["task"].toString(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                }


                                                Box(
                                                    modifier = Modifier.height(40.dp)
                                                        .width(80.dp)
                                                        .border(width = 2.dp, color = Color.Blue),
                                                    contentAlignment = Alignment.Center
                                                )
                                                {
                                                    Icon(painter = painterResource(id = R.drawable.bolt), contentDescription = "user",tint = Color.Black, )
                                                }

                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(30.dp)
                                            .border(width = 2.dp, color = Color.Blue),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    )
                                    {

                                        Icon(painter = painterResource(id = R.drawable.info), contentDescription = "user",tint = if(myShareState[0]) fontColor else Color.Black,modifier = Modifier.clickable {
                                            myShareState[0] = ! myShareState[0]
                                            myShareState[1] = false

                                        })
                                        Icon(painter = painterResource(id = R.drawable.change), contentDescription = "user",tint = if(myShareState[1]) fontColor else Color.Black,modifier = Modifier.clickable {
                                            myShareState[0] = false
                                            myShareState[1] = ! myShareState[1]
                                        })
                                    }



                                    Column(
                                        modifier = Modifier.fillMaxWidth()
                                            .border(width = 2.dp, color = Color.Blue)
                                            .height(230.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceAround
                                    )
                                    {
                                        if(myShareState[0])
                                        {
                                            if(selectedIndex != -1)
                                            {
                                                Box(
                                                    modifier = Modifier.fillMaxWidth()
                                                        .background(color = fontColor, shape = RoundedCornerShape(12.dp))
                                                        .height(50.dp),
                                                    contentAlignment = Alignment.CenterStart

                                                )
                                                {
                                                    Text(text="TASK TAKER: $giver" , modifier = Modifier.padding(20.dp,0.dp), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Box(
                                                    modifier = Modifier.fillMaxWidth()
                                                        .background(color = fontColor, shape = RoundedCornerShape(12.dp))
                                                        .height(110.dp),
                                                    contentAlignment = Alignment.CenterStart

                                                )
                                                {
                                                    Text(text="TASK: $taskName" , modifier = Modifier.padding(20.dp,0.dp), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                                }
                                                val fieldSize = ((screenWidth - 60.dp)*4)/10
                                                val space = (screenWidth - 60.dp) - 2*fieldSize

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(space,Alignment.Start),
                                                    verticalAlignment = Alignment.CenterVertically
                                                )
                                                {
                                                    Box(
                                                        modifier = Modifier.width(fieldSize)
                                                            .background(color = fontColor, shape = RoundedCornerShape(12.dp))
                                                            .height(50.dp),
                                                        contentAlignment = Alignment.CenterStart

                                                    )
                                                    {
                                                        Text(text="KARMA POINTS: $karmaPoints" , modifier = Modifier.padding(15.dp,0.dp), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                                    }

                                                    Box(
                                                        modifier = Modifier.width(fieldSize)
                                                            .background(color = fontColor, shape = RoundedCornerShape(12.dp))
                                                            .height(50.dp),
                                                        contentAlignment = Alignment.CenterStart

                                                    )
                                                    {
                                                        Text(text="TIMING: $time" , modifier = Modifier.padding(15.dp,0.dp), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                        else if(myShareState[1])
                                        {
                                            Box(
                                                modifier = Modifier.fillMaxWidth()
                                                    .background(color = fontColor, shape = RoundedCornerShape(12.dp))
                                                    .height(50.dp),
                                                contentAlignment = Alignment.CenterStart

                                            )
                                            {
                                                Text(text="TASK TAKER: $giver" , modifier = Modifier.padding(20.dp,0.dp), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Box(
                                                modifier = Modifier.fillMaxWidth()
                                                    .background(color = fontColor, shape = RoundedCornerShape(12.dp))
                                                    .height(110.dp),
                                                contentAlignment = Alignment.CenterStart

                                            )
                                            {
                                                Text(text="TASK: $taskName" , modifier = Modifier.padding(20.dp,0.dp), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                            }
                                            val fieldSize = ((screenWidth - 60.dp)*4)/10
                                            val space = (screenWidth - 60.dp) - 2*fieldSize

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(space,Alignment.Start),
                                                verticalAlignment = Alignment.CenterVertically
                                            )
                                            {
                                                Box(
                                                    modifier = Modifier.width(fieldSize)
                                                        .background(color = fontColor, shape = RoundedCornerShape(12.dp))
                                                        .height(50.dp),
                                                    contentAlignment = Alignment.CenterStart

                                                )
                                                {
                                                    Text(text="KARMA POINTS: $karmaPoints" , modifier = Modifier.padding(15.dp,0.dp), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                                }

                                                Icon(painter = painterResource(id = R.drawable.remove), contentDescription = "user",tint = if(myShareState[0]) fontColor else Color.Black,modifier = Modifier.clickable {

                                                })
                                            }

                                        }

                                    }

                                }

                            }

                            else if (state[4])
                            {
                                val tasks = remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

                                var taskName by remember { mutableStateOf("") }
                                var giver by remember { mutableStateOf("") }
                                var taskIndex by remember { mutableStateOf("") }


                                // Call retrieveTasks when the screen is loaded or refreshed
                                LaunchedEffect(Unit) {
                                    while (true) {
                                        if (username != null) {
                                            retrieveTasks(user = username, serverUrl = serverUrl) { fetchedTasks ->
                                                tasks.value = fetchedTasks
                                            }
                                        }
                                        delay(300)
                                    }
                                }

//                                val index = 0 // Change this to the index you want to access
//                                val taskAtIndex = tasks.value.getOrNull(index)
//                                val usernameAtIndex = taskAtIndex?.get("username")?.toString()
//
//                                if (usernameAtIndex != null) {
//                                    Text(text = "Username at index $index: $usernameAtIndex")
//                                } else {
//                                    Text(text = "No task at index $index")
//                                }

                                // Display tasks in a table

                                var selectedIndex by remember { mutableIntStateOf(-1) }

                                Column(
                                    modifier = Modifier.padding(10.dp)
                                        .height(580.dp)
                                        .border(width = 2.dp, color = Color.Blue),
                                    horizontalAlignment = Alignment.CenterHorizontally,)
                                {
                                    Row(
                                        modifier = Modifier
                                            .padding(10.dp)
                                            .fillMaxWidth()
                                            .height(40.dp)
                                            .background(color = headerColor,shape = RoundedCornerShape(12.dp))
                                        ,
                                        horizontalArrangement = Arrangement.spacedBy(30.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier.height(40.dp)
                                                .width(30.dp)
                                                .border(width = 2.dp, color = Color.Blue),
                                            contentAlignment = Alignment.Center
                                        )
                                        {
                                            Text(text = "s.no", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        }

                                        Box(
                                            modifier = Modifier.height(40.dp)
                                                .width(120.dp)
                                                .border(width = 2.dp, color = Color.Blue),
                                            contentAlignment = Alignment.CenterStart
                                        )
                                        {
                                            Text(text = "global - users", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        }

                                        Box(
                                            modifier = Modifier.height(40.dp)
                                                .width(100.dp)
                                                .border(width = 2.dp, color = Color.Blue),
                                            contentAlignment = Alignment.Center
                                        )
                                        {
                                            Text(text = "karma - points", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        }


                                    }

                                        LazyColumn(
                                            modifier = Modifier.height(330.dp)
                                                .border(width = 2.dp, color = Color.Red),
                                            verticalArrangement = Arrangement.spacedBy(5.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            itemsIndexed(tasks.value) { index, task ->

                                                val backgroundColor = if (selectedIndex == index) fontColor else taskColor

                                                Row(
                                                    modifier = Modifier
                                                        .padding(10.dp)
                                                        .fillMaxWidth()
                                                        .height(40.dp)
                                                        .background(color = backgroundColor,shape = RoundedCornerShape(12.dp))
                                                        .clickable {

                                                        selectedIndex = index
                                                        // Handle row click
                                                            taskName = task["task"].toString()
                                                            giver = task["username"].toString()
                                                            taskIndex = task["taskIndex"].toString()

                                                            Toast.makeText(
                                                                this@MainActivity2,
                                                                task["taskIndex"].toString(),
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                    }
                                                        ,
                                                    horizontalArrangement = Arrangement.spacedBy(30.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier.height(40.dp)
                                                            .width(30.dp)
                                                            .border(width = 2.dp, color = Color.Blue),
                                                        contentAlignment = Alignment.Center
                                                    )
                                                    {
                                                        Text(text = (index+1).toString()+".", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                    }

                                                    Box(
                                                        modifier = Modifier.height(40.dp)
                                                            .width(120.dp)
                                                            .border(width = 2.dp, color = Color.Blue),
                                                        contentAlignment = Alignment.CenterStart
                                                    )
                                                    {
                                                        Text(text = task["username"].toString(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                    }

                                                    Box(
                                                        modifier = Modifier.height(40.dp)
                                                            .width(100.dp)
                                                            .border(width = 2.dp, color = Color.Blue),
                                                        contentAlignment = Alignment.Center
                                                    )
                                                    {
                                                        Text(text = task["karmaPoints"].toString(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                    }

                                                }
                                            }
                                        }



                                    Column(
                                        modifier = Modifier.fillMaxWidth()
                                            .border(width = 2.dp, color = Color.Blue)
                                            .height(210.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceAround
                                    )
                                    {

                                        Box(
                                            modifier = Modifier.fillMaxWidth()
                                                .background(color = fontColor, shape = RoundedCornerShape(12.dp))
                                                .height(100.dp),
                                            contentAlignment = Alignment.CenterStart

                                        )
                                        {
                                            Text(text="TASK: $taskName" , modifier = Modifier.padding(20.dp,0.dp), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                        }

                                        val fieldSize = ((screenWidth - 60.dp)*4)/10
                                        val space = (screenWidth - 60.dp) - 2*fieldSize - 30.dp

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(space/2,Alignment.Start),
                                            verticalAlignment = Alignment.CenterVertically
                                        )
                                        {
                                            var karmaRange by remember { mutableIntStateOf(0) }


                                            TextField(
                                                value = "0 - " + karmaRange.toString(),
                                                onValueChange = { karmaRange = it.toInt() },
                                                enabled = false ,
                                                label = { Text(text = "Karma Points",color = Color.Black) },
                                                modifier = Modifier
                                                    .width(fieldSize),
                                                textStyle = TextStyle.Default.copy(
                                                    fontSize = 20.sp,
                                                    color = Color.Black,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                colors = TextFieldDefaults.textFieldColors(
                                                    unfocusedLabelColor = Color.Transparent,
                                                    focusedLabelColor = Color.Transparent,
                                                    cursorColor = Color.Black,
                                                    focusedLeadingIconColor = Color.White,
                                                    focusedIndicatorColor = Color.Transparent,
                                                    unfocusedIndicatorColor = Color.Transparent,
                                                    disabledIndicatorColor = Color.Transparent,
                                                    unfocusedLeadingIconColor = taskColor,
                                                    containerColor = taskColor,
                                                    focusedTrailingIconColor = Color.White,
                                                    unfocusedTrailingIconColor = Color.White,
                                                ),
                                                shape = RoundedCornerShape(10.dp),
                                                trailingIcon = {
                                                    Icon(Icons.Default.Add, contentDescription = "Select Date" ,tint = Color.Black , modifier = Modifier.clickable {
                                                        karmaRange+=1
                                                    })
                                                }
                                            )

                                            Icon(painter = painterResource(id = R.drawable.takeuo), contentDescription = "user",tint = Color.Black, modifier = Modifier.size(30.dp)
                                                .clickable{
                                                    if (username != null) {
                                                        tasksUpdater(serverUrl = serverUrl,giver = giver ,taker = username ,taskNumber = taskIndex ,karmaPoints = karmaPoints )
                                                    }
                                                })

                                            Box(
                                                modifier = Modifier.width(fieldSize)
                                                    .background(color = fontColor, shape = RoundedCornerShape(12.dp))
                                                    .height(50.dp),
                                                contentAlignment = Alignment.CenterStart

                                            )
                                            {
                                                Text(text="TIMING: $taskDescription" , modifier = Modifier.padding(15.dp,0.dp), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                    }

                                }

                            }
                    }

                }

                    else if (mainState[2])
                    {
                        val tasks = remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

                        var comments by remember { mutableStateOf("") }
                        var commentType by remember { mutableStateOf("") }

                        var selectedIndex by remember { mutableIntStateOf(-1) }

//                        allTasks.add(mapOf(" commentTo" to  commentTo,"commentFrom" to commentFrom, "dateAndTime" to dateAndTime,"comments" to comments,"commentType" to commentType))


                        // Call retrieveTasks when the screen is loaded or refreshed
                        LaunchedEffect(Unit) {
                            while (true) {
                                if (username != null) {
                                    retrieveUserComments(user = username, serverUrl = serverUrl) { fetchedTasks ->
                                        tasks.value = fetchedTasks
                                    }
                                }
                                delay(300)
                            }
                        }

                        Column(
                            modifier = Modifier.padding(10.dp)
                                .height(580.dp)
                                .border(width = 2.dp, color = Color.Blue),
                            horizontalAlignment = Alignment.CenterHorizontally,)
                        {
                            Row(
                                modifier = Modifier
                                    .padding(10.dp)
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .background(color = headerColor,shape = RoundedCornerShape(12.dp))
                                ,
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.height(40.dp)
                                        .width(80.dp)
                                        .border(width = 2.dp, color = Color.Blue),
                                    contentAlignment = Alignment.Center
                                )
                                {
                                    Text(text = "from", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }

                                Box(
                                    modifier = Modifier.height(40.dp)
                                        .width(80.dp)
                                        .border(width = 2.dp, color = Color.Blue),
                                    contentAlignment = Alignment.Center
                                )
                                {
                                    Text(text = "to", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }

                                Box(
                                    modifier = Modifier.height(40.dp)
                                        .width(100.dp)
                                        .border(width = 2.dp, color = Color.Blue),
                                    contentAlignment = Alignment.Center
                                )
                                {
                                    Text(text = "time", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }


                            }

                            LazyColumn(
                                modifier = Modifier.height(280.dp)
                                    .border(width = 2.dp, color = Color.Red),
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                itemsIndexed(tasks.value) { index, task ->

                                    val backgroundColor = if (selectedIndex == index) fontColor else taskColor

                                    Row(
                                        modifier = Modifier
                                            .padding(10.dp)
                                            .fillMaxWidth()
                                            .height(40.dp)
                                            .background(color = backgroundColor,shape = RoundedCornerShape(12.dp))
                                            .clickable {

                                                selectedIndex = index
                                                // Handle row click
                                                commentType = task["commentType"].toString()
                                                comments = task["comments"].toString()

//                                                Toast.makeText(
//                                                    this@MainActivity2,
//                                                    task["taskIndex"].toString(),
//                                                    Toast.LENGTH_SHORT
//                                                ).show()
                                            }
                                        ,
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier.height(40.dp)
                                                .width(80.dp)
                                                .border(width = 2.dp, color = Color.Blue),
                                            contentAlignment = Alignment.Center
                                        )
                                        {
                                            Text(text = task["commentFrom"].toString(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        }

                                        Box(
                                            modifier = Modifier.height(40.dp)
                                                .width(80.dp)
                                                .border(width = 2.dp, color = Color.Blue),
                                            contentAlignment = Alignment.Center
                                        )
                                        {
                                            Text(text = task["commentTo"].toString(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        }

                                        Box(
                                            modifier = Modifier.height(40.dp)
                                                .width(100.dp)
                                                .border(width = 2.dp, color = Color.Blue),
                                            contentAlignment = Alignment.Center
                                        )
                                        {
                                            Text(text = task["dateAndTime"].toString(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        }

                                    }
                                }
                            }



                            Column(
                                modifier = Modifier.fillMaxWidth()
                                    .border(width = 2.dp, color = Color.Blue)
                                    .height(260.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceAround
                            )
                            {

                                Box(
                                    modifier = Modifier.fillMaxWidth()
                                        .background(color = fontColor, shape = RoundedCornerShape(12.dp))
                                        .height(50.dp),
                                    contentAlignment = Alignment.CenterStart
                                )
                                {
                                    Text(text="COMMENT TYPE: $commentType  " , modifier = Modifier.padding(20.dp,0.dp), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }

                                Box(
                                    modifier = Modifier.fillMaxWidth()
                                        .background(color = fontColor, shape = RoundedCornerShape(12.dp))
                                        .height(50.dp),
                                    contentAlignment = Alignment.CenterStart

                                )
                                {
                                    Text(text="TASK RATING: $comments " , modifier = Modifier.padding(20.dp,0.dp), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }

                                Box(
                                    modifier = Modifier.fillMaxWidth()
                                        .background(color = fontColor, shape = RoundedCornerShape(12.dp))
                                        .height(100.dp),
                                    contentAlignment = Alignment.CenterStart

                                )
                                {
                                    Text(text="TASK: $comments " , modifier = Modifier.padding(20.dp,0.dp), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }

                            }
                    }

            }

                    else if(mainState[3])
                    {

                        var username by remember { mutableStateOf("") }
                        var currentpassword by remember { mutableStateOf("") }
                        var newPassword by remember { mutableStateOf("") }
                        var currentpasswordVisible by remember { mutableStateOf(false) }
                        var newPasswordVisible by remember { mutableStateOf(false) }
                        var dateOfBirth by remember { mutableStateOf("") }
                        var mobileNumber by remember { mutableStateOf("") }
                        var gender by remember { mutableStateOf("") }

                        var selectedDate by remember { mutableStateOf("") }
                        val context = LocalContext.current

                        dateOfBirth = selectedDate

                        val calendar = Calendar.getInstance()
                        val year = calendar.get(Calendar.YEAR)
                        val month = calendar.get(Calendar.MONTH)
                        val day = calendar.get(Calendar.DAY_OF_MONTH)

                        val datePickerDialog = DatePickerDialog(
                            context,
                            { _, selectedYear, selectedMonth, selectedDay ->
                                selectedDate = "${selectedDay}/${selectedMonth + 1}/${selectedYear}"
                            },
                            year,
                            month,
                            day
                        )
                        Column(
                            modifier = Modifier.padding(10.dp)
                                .height(580.dp)
                                .border(width = 2.dp, color = Color.Blue),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceEvenly

                            )
                        {
                            TextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text(text = "username",color = Color.Black) },
                                enabled = false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp),
                                textStyle = TextStyle.Default.copy(
                                    fontSize = 20.sp,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                ),
                                colors = TextFieldDefaults.textFieldColors(
                                    unfocusedLabelColor = Color.Transparent,
                                    focusedLabelColor = Color.Transparent,
                                    cursorColor = Color.Black,
                                    focusedLeadingIconColor = Color.White,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent,
                                    unfocusedLeadingIconColor = Color.White,
                                    containerColor = taskColor,
                                    focusedTrailingIconColor = Color.White,
                                    unfocusedTrailingIconColor = Color.White,
                                ),
                                shape = RoundedCornerShape(10.dp),
                                trailingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = "person" ,tint = fontColor,modifier = Modifier.size(30.dp))
                                }
                            )

                            TextField(
                                value = mobileNumber,
                                onValueChange = { mobileNumber = it },
                                label = { Text(text = "mobile number",color = Color.Black) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp),
                                textStyle = TextStyle.Default.copy(
                                    fontSize = 20.sp,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                ),
                                colors = TextFieldDefaults.textFieldColors(
                                    unfocusedLabelColor = Color.Transparent,
                                    focusedLabelColor = Color.Transparent,
                                    cursorColor = Color.Black,
                                    focusedLeadingIconColor = Color.White,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent,
                                    unfocusedLeadingIconColor = Color.White,
                                    containerColor = taskColor,
                                    focusedTrailingIconColor = Color.White,
                                    unfocusedTrailingIconColor = Color.White,
                                ),
                                shape = RoundedCornerShape(10.dp),
                                trailingIcon = {
                                    Icon(painter = painterResource(id = R.drawable.mobile), contentDescription = "user",tint =  fontColor , modifier = Modifier.size(30.dp))
                                }
                            )


                            TextField(
                                colors = TextFieldDefaults.textFieldColors(
                                    unfocusedLabelColor = Color.Transparent,
                                    focusedLabelColor = Color.Transparent,
                                    cursorColor = Color.White,
                                    focusedLeadingIconColor = Color.White,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent,
                                    unfocusedLeadingIconColor = Color.White,
                                    containerColor = taskColor,
                                    focusedTrailingIconColor = Color.White,
                                    unfocusedTrailingIconColor = Color.White
                                ),
                                value = selectedDate,
                                onValueChange = {
                                },
                                readOnly = true,
                                shape = RoundedCornerShape(10.dp),
                                textStyle = TextStyle.Default.copy(
                                    fontSize = 20.sp,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                ),
                                label = { Text(text = "Date of Birth",color = Color.Black) },
                                trailingIcon = {
                                    IconButton(onClick = { datePickerDialog.show() }) {
                                        Icon(Icons.Default.DateRange, contentDescription = "Select Date" ,tint = fontColor,modifier = Modifier.size(30.dp))
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp)
                            )

                            var isExpanded by remember { mutableStateOf(false) }
                            val genderOptions = listOf("Male", "Female","Others")
                            var selectedText by remember { mutableStateOf("") }

                            ExposedDropdownMenuBox(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp),
                                expanded = isExpanded,
                                onExpandedChange = { isExpanded = !isExpanded }
                            ) {
                                TextField(
                                    value = selectedText,
                                    onValueChange = {},
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                    ,
                                    readOnly = true,
                                    enabled = false,
                                    label = { Text("gender",color = Color.Black) },
                                    textStyle = TextStyle.Default.copy(
                                        fontSize = 20.sp,
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    colors = TextFieldDefaults.textFieldColors(
                                        unfocusedLabelColor = Color.Transparent,
                                        focusedLabelColor = Color.Transparent,
                                        cursorColor = Color.White,
                                        focusedLeadingIconColor = Color.White,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        disabledIndicatorColor = Color.Transparent,
                                        unfocusedLeadingIconColor = Color.White,
                                        containerColor = taskColor,
                                        focusedTrailingIconColor = Color.White,
                                        unfocusedTrailingIconColor = Color.White,
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    trailingIcon = {
                                        if(!isExpanded)
                                        {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Dropdown Icon",
                                                tint = fontColor,
                                                modifier = Modifier.size(30.dp)
                                            )
                                        }
                                        else
                                        {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowUp,
                                                contentDescription = "Dropdown Icon",
                                                tint = fontColor,
                                                modifier = Modifier.size(30.dp)
                                            )
                                        }

                                    }
                                )

                                ExposedDropdownMenu(
                                    expanded = isExpanded,
                                    onDismissRequest = { isExpanded = false }
                                ) {
                                    genderOptions.forEachIndexed { index, text ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = text,
                                                    style = TextStyle.Default.copy(
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                            },
                                            onClick = {
                                                selectedText = genderOptions[index]
                                                gender = genderOptions[index]
                                                isExpanded = false
                                            },
                                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                        )
                                    }
                                }
                            }

                            TextField(
                                value = currentpassword,
                                onValueChange = { currentpassword = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp),
                                label = { Text("current-password",color = Color.Black) },
                                textStyle = TextStyle.Default.copy(
                                    fontSize = 20.sp,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                ),
                                visualTransformation = if (currentpasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                colors = TextFieldDefaults.textFieldColors(
                                    unfocusedLabelColor = Color.Transparent,
                                    focusedLabelColor = Color.Transparent,
                                    cursorColor = Color.White,
                                    focusedLeadingIconColor = Color.White,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent,
                                    unfocusedLeadingIconColor = Color.White,
                                    containerColor = taskColor,
                                    focusedTrailingIconColor = Color.White,
                                    unfocusedTrailingIconColor = Color.White,
                                ),
                                shape = RoundedCornerShape(10.dp),
                                trailingIcon = {
                                    val image = if (currentpasswordVisible)
                                        painterResource(id = R.drawable.visibility)
                                    else
                                        painterResource(id = R.drawable.visibility_off)

                                    IconButton(onClick = {
                                        currentpasswordVisible = !currentpasswordVisible
                                    }) {
                                        Icon(painter = image, contentDescription = if (passwordVisible) "Hide password" else "Show password",tint = fontColor, modifier = Modifier.size(30.dp))
                                    }
                                }
                            )

                            TextField(
                                value = newPassword,
                                onValueChange = { newPassword = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp),
                                label = { Text("new-password",color = Color.Black) },
                                textStyle = TextStyle.Default.copy(
                                    fontSize = 20.sp,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                ),
                                visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                colors = TextFieldDefaults.textFieldColors(
                                    unfocusedLabelColor = Color.Transparent,
                                    focusedLabelColor = Color.Transparent,
                                    cursorColor = Color.White,
                                    focusedLeadingIconColor = Color.White,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent,
                                    unfocusedLeadingIconColor = Color.White,
                                    containerColor = taskColor,
                                    focusedTrailingIconColor = Color.White,
                                    unfocusedTrailingIconColor = Color.White,
                                ),
                                shape = RoundedCornerShape(10.dp),
                                trailingIcon = {
                                    val image = if (newPasswordVisible)
                                        painterResource(id = R.drawable.visibility)
                                    else
                                        painterResource(id = R.drawable.visibility_off)

                                    IconButton(onClick = {
                                        newPasswordVisible = !newPasswordVisible
                                    }) {
                                        Icon(painter = image, contentDescription = if (newPasswordVisible) "Hide password" else "Show password",tint = fontColor, modifier = Modifier.size(30.dp))
                                    }
                                }
                            )

                            Box(
                                modifier = Modifier
                                    .background(color = fontColor, shape = RoundedCornerShape(size = 12.dp))
                                    .clickable {

                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "edit", fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(10.dp))
                            }
                        }


                    }
        }



}
    }
        }}







