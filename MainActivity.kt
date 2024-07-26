package com.example.loginpage

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.icu.util.Calendar
import android.os.Bundle
import android.service.autofill.OnClickAction
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONStringer
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

var userState = mutableStateListOf<Boolean>(false,false,false)

var username by  mutableStateOf("")
var userstat by  mutableStateOf("")
var passwordVisible by  mutableStateOf(false)
var confirmPasswordVisible by mutableStateOf(false)
var password by  mutableStateOf("")
var confirmPassword  by mutableStateOf("")
var age by  mutableStateOf("")
var gender by  mutableStateOf("")

var state  = mutableStateListOf<Boolean>(false,false,false,false,false)


class MainActivity : ComponentActivity() {

    private val serverUrl = "http://10.2.9.126:3000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
            val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)

            if (isLoggedIn) {
                val username  = sharedPreferences.getString("username", "lol")
                val password  = sharedPreferences.getString("password", "lol")

                if (username != null && password != null ) {
                    loginUser(serverUrl = serverUrl , username = username , password = password)
                }

            }
            else
            {
                MainScreen(serverUrl)
            }

        }
    }

    private fun registerUser(username: String, passwordPrivate: String,gender: String,dateOfBirth: String,serverUrl: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("$serverUrl/register")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8")
                conn.setRequestProperty("Accept", "application/json")
                conn.doOutput = true

                val jsonParam = JSONObject().apply {
                    put("username", username)
                    put("password", passwordPrivate)
                    put("age", dateOfBirth)
                    put("gender", gender)
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
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                        if (jsonObject.has("userId")) {
                            userState[1] = true
                            password = ""
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {

                        if(responseCode == 409)
                        {
                            Toast.makeText(this@MainActivity, "User Already Exists", Toast.LENGTH_SHORT).show()
                        }
                        else
                        {
                            Toast.makeText(this@MainActivity, "$responseCode", Toast.LENGTH_SHORT).show()
                        }
                        Log.d("Registration Error", "$responseCode")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.d("Registration Error", "${e.message}")
                }
            }
        }
    }




    private fun loginUser(username: String, password: String, serverUrl: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("$serverUrl/login")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8")
                conn.setRequestProperty("Accept", "application/json")
                conn.doOutput = true

                val jsonParam = JSONObject().apply {
                    put("username", username)
                    put("password", password)
                }

                Log.d("LoginActivity", "JSON Request: $jsonParam")

                val os = OutputStreamWriter(conn.outputStream)
                os.write(jsonParam.toString())
                os.flush()
                os.close()

                val responseCode = conn.responseCode
                Log.d("LoginActivity", "Response Code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
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
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                        if (jsonObject.has("user")) {

                            val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                            val editor = sharedPreferences.edit()
                            editor.putBoolean("isLoggedIn", true)
                            editor.putString("username", username)
                            editor.putString("password", password)
                            editor.apply()

                            val userJson = jsonObject.getJSONObject("user")
                            val intent = Intent(this@MainActivity, MainActivity2::class.java)
                            intent.putExtra("username", username)
                            intent.putExtra("age", userJson.getString("age"))
                            intent.putExtra("gender", userJson.getString("gender"))
                            intent.putExtra("karma points", userJson.getString("karmaPoints"))
                            startActivity(intent)
                            finish()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Error: ${responseCode}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.d("Login Error", "${e.message}")
                }
            }
        }
    }


    private fun multipleUserChecker(username: String, serverUrl: String, onResult: (String) -> Unit) {

        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("token", null)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("$serverUrl/login-stat")
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

                            for (i in 0 until jsonArray.length()) {
                                val userObject = jsonArray.getJSONObject(i)
                                val userList = userObject.getString("username")

                                if (userList == username) {
                                    val userstat = userObject.getString("userOnline")
                                    onResult(userstat)
                                    return@withContext
                                }
                            }

                            onResult("not found")
                        } catch (e: JSONException) {
                            Log.e("API_RESPONSE", "JSON Parsing Error: ${e.message}")
                            Toast.makeText(this@MainActivity, "Error parsing tasks", Toast.LENGTH_SHORT).show()
                            onResult("error")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Error: $responseCode", Toast.LENGTH_SHORT).show()
                        onResult("error")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    onResult("error")
                }
            }
        }
    }



                @OptIn(ExperimentalMaterial3Api::class)
                @Composable
                fun MainScreen(serverUrl: String) {


                    val backgroundColor = Color(0xFFADBADA)
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = backgroundColor
                    ) {
                        Column(
                            verticalArrangement = Arrangement.SpaceEvenly,
                            horizontalAlignment = Alignment.CenterHorizontally
                        )
                        {


                            val innerColor = Color(0xFF8697C3)
                            val fontColor = Color(0xFF3d52a1)
                            val taskColor = Color(0xFFadbada)
                            val headerColor = Color(0xFFeee8f6)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .padding(20.dp, 0.dp)
                                    .background(
                                        color = innerColor,
                                        shape = RoundedCornerShape(20.dp)
                                    ),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            )
                            {
                                Icon(
                                    painter = painterResource(id = R.drawable.login),
                                    contentDescription = "user",
                                    tint = if (userState[0]) fontColor else Color.Black,
                                    modifier = Modifier.clickable {
                                        userState[0] = !userState[0]
                                        userState[1] = false
                                        userState[2] = false

                                    })
                                Icon(
                                    painter = painterResource(id = R.drawable.signup),
                                    contentDescription = "user",
                                    tint = if (userState[1]) fontColor else Color.Black,
                                    modifier = Modifier.clickable {
                                        userState[0] = false
                                        userState[1] = !userState[1]
                                        userState[2] = false
                                    })

                                Icon(
                                    painter = painterResource(id = R.drawable.setting),
                                    contentDescription = "user",
                                    tint = if (userState[2]) fontColor else Color.Black,
                                    modifier = Modifier.clickable {
                                        userState[0] = false
                                        userState[1] = false
                                        userState[2] = !userState[2]
                                    })

                            }


                            Box(
                                modifier = Modifier
                                    .height(650.dp)
                                    .fillMaxWidth()
                                    .padding(20.dp, 0.dp)
                                    .background(
                                        color = innerColor,
                                        shape = RoundedCornerShape(20.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            )
                            {
                                Column(
                                    modifier = Modifier.fillMaxSize()
                                        .padding(10.dp)
                                        .border(width = 2.dp, color = Color.Blue),
                                    verticalArrangement = if(userState[0]) Arrangement.spacedBy(20.dp,Alignment.CenterVertically) else Arrangement.SpaceEvenly,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                )
                                {
                                    if(userState[0])
                                    {
                                        var username by remember { mutableStateOf("") }
                                        var password by remember { mutableStateOf("") }
                                        var passwordVisible by remember { mutableStateOf(false) }

                                        TextField(
                                            value = username,
                                            onValueChange = { username = it },
                                            label = { Text(text = "Username",color = Color.Black) },
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
                                            value = password,
                                            onValueChange = { password = it },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp),
                                            label = { Text("Password",color = Color.Black) },
                                            textStyle = TextStyle.Default.copy(
                                                fontSize = 20.sp,
                                                color = Color.Black,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
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
                                                val image = if (passwordVisible)
                                                    painterResource(id = R.drawable.visibility)
                                                else
                                                    painterResource(id = R.drawable.visibility_off)

                                                IconButton(onClick = {
                                                    passwordVisible = !passwordVisible
                                                }) {
                                                    Icon(painter = image, contentDescription = if (passwordVisible) "Hide password" else "Show password",tint = fontColor, modifier = Modifier.size(30.dp))
                                                }
                                            }
                                        )

                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        )
                                        {

                                                Button(
                                                    modifier = Modifier.padding(horizontal = 5.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = fontColor ),
                                                    shape = RoundedCornerShape(10.dp),
                                                    onClick = {
                                                        userState[0] = false
                                                        userState[1] = !userState[1]
                                                        userState[2] = false
                                                        username = ""
                                                        passwordVisible = false
                                                        confirmPasswordVisible = false
                                                        password = ""
                                                        confirmPassword = ""
                                                        age = ""
                                                        gender = ""
                                                    })
                                                {
                                                    Text(text = "New - User",color = Color.Black)
                                                }

                                            Button(
                                                modifier = Modifier
                                                    .padding(horizontal = 5.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = fontColor ),
                                                shape = RoundedCornerShape(10.dp),
                                                onClick = {
                                                        multipleUserChecker(username, serverUrl) { stat ->
                                                            if(stat == "0")
                                                            {
                                                                loginUser(username = username, password = password,serverUrl = serverUrl)
                                                            }
                                                        }
                                                })
                                            {
                                                Text(text = "Login",color = Color.Black)
                                            }
                                        }

                                    }

                                    else if(userState[1])
                                    {
                                        var username by remember { mutableStateOf("") }
                                        var password by remember { mutableStateOf("") }
                                        var confirmPassword by remember { mutableStateOf("") }
                                        var passwordVisible by remember { mutableStateOf(false) }
                                        var confirmPasswordVisible by remember { mutableStateOf(false) }
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

                                        TextField(
                                            value = username,
                                            onValueChange = { username = it },
                                            label = { Text(text = "username",color = Color.Black) },
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
                                            value = password,
                                            onValueChange = { password = it },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp),
                                            label = { Text("password",color = Color.Black) },
                                            textStyle = TextStyle.Default.copy(
                                                fontSize = 20.sp,
                                                color = Color.Black,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
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
                                                val image = if (passwordVisible)
                                                    painterResource(id = R.drawable.visibility)
                                                else
                                                    painterResource(id = R.drawable.visibility_off)

                                                IconButton(onClick = {
                                                    passwordVisible = !passwordVisible
                                                }) {
                                                    Icon(painter = image, contentDescription = if (passwordVisible) "Hide password" else "Show password",tint = fontColor, modifier = Modifier.size(30.dp))
                                                }
                                            }
                                        )

                                        TextField(
                                            value = confirmPassword,
                                            onValueChange = { confirmPassword = it },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp),
                                            label = { Text("confirm-password",color = Color.Black) },
                                            textStyle = TextStyle.Default.copy(
                                                fontSize = 20.sp,
                                                color = Color.Black,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
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
                                                val image = if (confirmPasswordVisible)
                                                    painterResource(id = R.drawable.visibility)
                                                else
                                                    painterResource(id = R.drawable.visibility_off)

                                                IconButton(onClick = {
                                                    confirmPasswordVisible = !confirmPasswordVisible
                                                }) {
                                                    Icon(painter = image, contentDescription = if (passwordVisible) "Hide password" else "Show password",tint = fontColor, modifier = Modifier.size(30.dp))
                                                }
                                            }
                                        )

                                        Box(
                                            modifier = Modifier
                                                .background(color = fontColor, shape = RoundedCornerShape(size = 12.dp))
                                                .clickable {
                                                    userState[1] = false
                                                    userState[0] = true
                                                    userState[2] = false

                                                    if(password.isNotEmpty() && confirmPassword.isNotEmpty() && password == confirmPassword)
                                                    {
                                                        registerUser(username = username,passwordPrivate = password,gender = gender,dateOfBirth = dateOfBirth,serverUrl =serverUrl)
                                                    }
                                                    else if(password.isNotEmpty() && confirmPassword.isNotEmpty())
                                                    {
                                                        Toast.makeText(this@MainActivity, "passwords doesn't match ", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = "Sign-up", fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(10.dp))
                                        }

                                    }

                                }

                            }
                        }
                    }
                }




    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Screen(serverUrl: String) {
        val configuration = LocalConfiguration.current
        val screenWidthDp = configuration.screenWidthDp.dp
        val screenHeightDp = configuration.screenHeightDp.dp



        val space = (screenHeightDp - 730.dp) / 3

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color.LightGray),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space)
        ) {
            Box(
                modifier = Modifier
                    .padding(15.dp)
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(color = Color.Gray, shape = RoundedCornerShape(size = 12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "LOGIN PAGE", fontSize = 30.sp, fontWeight = FontWeight.Bold)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(35.dp, Alignment.CenterHorizontally),
            ) {
                Box(
                    modifier = Modifier
                        .width(150.dp)
                        .height(40.dp)
                        .background(color =if(!userState[0]) Color.DarkGray else Color.Gray, shape = RoundedCornerShape(size = 12.dp))
                        .clickable {
                            userState[0] = false
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "SIGN-UP", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .width(150.dp)
                        .height(40.dp)
                        .background(color = if(userState[1]) Color.DarkGray else Color.Gray, shape = RoundedCornerShape(size = 12.dp))
                        .clickable {
                            userState[1] = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "LOG-IN", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                }

            }


            Box(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .height(600.dp)
                    .background(color = Color.Gray, shape = RoundedCornerShape(size = 12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(text = "Username",color = Color.Black) },
                        modifier = Modifier
                            .width(300.dp)
                            .padding(vertical = 20.dp),
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
                            Icon(Icons.Default.Person, contentDescription = "Select Date" ,tint = Color.Black)
                        }
                    )


                    if(!userState[0])
                    {
                        var selectedDate by remember { mutableStateOf("") }
                        val context = LocalContext.current

                        age = selectedDate

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
                                containerColor = Color.White,
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
                                    Icon(Icons.Default.DateRange, contentDescription = "Select Date" ,tint = Color.Black)
                                }
                            },
                            modifier = Modifier
                                .width(300.dp)
                                .padding(vertical = 20.dp)
                        )

                        var isExpanded by remember { mutableStateOf(false) }
                        val genderOptions = listOf("Male", "Female")
                        var selectedText by remember { mutableStateOf("") }

                        ExposedDropdownMenuBox(
                            modifier = Modifier
                                .width(300.dp)
                                .padding(vertical = 20.dp),
                            expanded = isExpanded,
                            onExpandedChange = { isExpanded = !isExpanded }
                        ) {
                            TextField(
                                value = selectedText,
                                onValueChange = {},
                                modifier = Modifier
                                    .menuAnchor()
                                    .width(300.dp),
                                readOnly = true,
                                enabled = false,
                                label = { Text("Gender",color = Color.Black) },
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
                                    containerColor = Color.White,
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
                                            tint = Color.Black
                                        )
                                    }
                                    else
                                    {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = "Dropdown Icon",
                                            tint = Color.Black
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
                    }

                    TextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier
                            .width(300.dp)
                            .padding(vertical = 20.dp),
                        label = { Text("Password",color = Color.Black) },
                        textStyle = TextStyle.Default.copy(
                            fontSize = 20.sp,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        ),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        colors = TextFieldDefaults.textFieldColors(
                            unfocusedLabelColor = Color.Transparent,
                            focusedLabelColor = Color.Transparent,
                            cursorColor = Color.White,
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
                            val image = if (passwordVisible)
                                painterResource(id = R.drawable.visibility)
                            else
                                painterResource(id = R.drawable.visibility_off)

                            IconButton(onClick = {
                                passwordVisible = !passwordVisible
                            }) {
                                Icon(painter = image, contentDescription = if (passwordVisible) "Hide password" else "Show password",tint = Color.Black)
                            }
                        }
                    )

                    if(!userState[0])
                    {
                        TextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            modifier = Modifier
                                .width(300.dp)
                                .padding(vertical = 20.dp),
                            label = { Text("Confirm-Password",color = Color.Black) },
                            textStyle = TextStyle.Default.copy(
                                fontSize = 20.sp,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            ),
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            colors = TextFieldDefaults.textFieldColors(
                                unfocusedLabelColor = Color.Transparent,
                                focusedLabelColor = Color.Transparent,
                                cursorColor = Color.White,
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
                                val image = if (confirmPasswordVisible)
                                    painterResource(id = R.drawable.visibility)
                                else
                                    painterResource(id = R.drawable.visibility_off)

                                IconButton(onClick = {
                                    confirmPasswordVisible = !confirmPasswordVisible
                                }) {
                                    Icon(painter = image, contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",tint = Color.Black)
                                }
                            }
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(space = 12.dp)
                    )
                    {
                        if(userState[1])
                        {
                            Button(
                                modifier = Modifier
                                    .padding(vertical = 10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White ),
                                shape = RoundedCornerShape(10.dp),
                                onClick = {
                                    userState[1] = false
                                    username = ""
                                    passwordVisible = false
                                    confirmPasswordVisible = false
                                    password = ""
                                    confirmPassword = ""
                                    age = ""
                                    gender = ""
                                })
                            {
                                Text(text = "New - User",color = Color.Black)
                            }
                        }
                        Button(
                            modifier = Modifier
                                .padding(vertical = 10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White ),
                            shape = RoundedCornerShape(10.dp),
                            onClick = {
                                if(userState[1])
                                {
                                    multipleUserChecker(username, serverUrl) { stat ->
                                        if(stat == "0")
                                        {
                                            loginUser(username, password, serverUrl)
                                        }
                                    }

                                }
                                else
                                {
                                    if(password.isNotEmpty() && confirmPassword.isNotEmpty() && password == confirmPassword)
                                    {
                                        registerUser(username, password,gender,age,serverUrl)
                                    }
                                    else if(password.isNotEmpty() && confirmPassword.isNotEmpty())
                                    {
                                        Toast.makeText(this@MainActivity, "passwords doesn't match ", Toast.LENGTH_SHORT).show()
                                    }
                                }

                            })
                        {
                            Text(text = if(!userState[0]) "Sign-Up" else "Login",color = Color.Black)
                        }
                    }


                }


            }

        }

    }
}
