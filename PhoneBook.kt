package contacts

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.io.File

abstract class Contact(mainName: String, phoneNumber: String) {
    var name: String = ""
        set(value) {
            field = value.trim()
        }

    var number: String = ""
        get() = if (field == "") "[no number]" else field
        set(value) {
            field = if (validNumber(value.trim())) {
                value.trim()
            } else {
                ""
            }
        }

    val created = Clock.System.now()
    var edited = Clock.System.now()

    init {
        name = mainName
        number = phoneNumber
    }

    companion object {
        fun validNumber(number: String): Boolean {
            return Regex("^\\+?\\([a-zA-Z0-9]+\\)([ -]+[a-zA-Z0-9]{2,})*$|" +
                    "^\\+?[a-zA-Z0-9]+[ -]\\([a-zA-Z0-9]{2,}\\)([ -]+[a-zA-Z0-9]{2,})*$|" +
                    "^\\+?[a-zA-Z0-9]+([ -]+[a-zA-Z0-9]{2,})*$").matches(number)
        }
    }

    open fun showName() = name

    abstract fun properties(): List<String>

    fun propertyValues(): Map<String, String> {
        val props = emptyMap<String, String>().toMutableMap()
        for (propertyName in properties()) {
            var clazz: Class<*>? = this.javaClass
            while (clazz != null) {
                try {
                    val property = clazz.getDeclaredField(propertyName)
                    property.isAccessible = true
                    val propertyValue = property.get(this)
                    props += propertyName to propertyValue.toString()
                    break
                } catch (e: NoSuchFieldException) {
                    clazz = clazz.superclass
                }
            }
        }
        return props
    }

    fun <T> setProperty(propertyName: String, value: T) {
        var clazz: Class<*>? = this.javaClass
        while (clazz != null) {
            try {
                val property = clazz.getDeclaredField(propertyName)
                property.isAccessible = true
                property.set(this, value)
                break
            } catch (e: NoSuchFieldException) {
                clazz = clazz.superclass
            }
        }
    }

    fun serialize(): String {
        var str = "{"
        for ((key, value) in propertyValues()) {
            str += "\"$key\": \"$value\", "
        }
        str += "\"created\": \"$created\", \"edited\": \"$edited\"}"
        return str
    }
}

class Person(name: String, lastName: String, birth: String, sex: String, number: String) : Contact(name, number) {

    private var surname = lastName.trim()
        set(value) {
            field = value.trim()
        }

    private var birthDate = birth.trim()
        get() = field.ifEmpty { "[no data]" }
        set(value) {
            field = value.trim()
        }

    private var gender = sex.trim()
        get() = field.ifEmpty { "[no data]" }
        set(value) {
            field = value.trim()
        }

    companion object {

        fun enterName() = print("Enter the name: ").run { readln() }

        fun enterSurname() = print("Enter the surname: ").run { readln() }

        fun enterBirthDate(): String {
            var birthDate = print("Enter the birth date: ").run { readln() }
            try {
                birthDate.toInstant()
            } catch (e: Exception) {
                println("Bad birth date!")
                birthDate = ""
            }
            return birthDate
        }

        fun enterGender(): String {
            var gender = print("Enter the gender (M, F): ").run { readln() }
            if (gender != "M" && gender != "F") {
                println("Bad gender!")
                gender = ""
            }
            return gender
        }

        fun enterPhoneNumber(): String {
            val number = print("Enter the number: ").run { readln().trim() }
            return if (validNumber(number)) {
                number
            } else {
                ""
            }
        }
    }

    override fun showName() = "$name $surname"

    override fun properties() = listOf("name", "surname", "birthDate", "gender", "number")

    override fun toString(): String {
        return "Name: $name\n" +
                "Surname: $surname\n" +
                "Birth date: $birthDate\n" +
                "Gender: $gender\n" +
                "Number: $number\n" +
                "Time created: ${created.toLocalDateTime(TimeZone.currentSystemDefault()).toString().dropLastWhile { it != ':' }.dropLast(1)}\n" +
                "Time last edit: ${edited.toLocalDateTime(TimeZone.currentSystemDefault()).toString().dropLastWhile { it != ':' }.dropLast(1)}"
    }
}

class Organization(name: String, private var address: String, number: String) : Contact(name, number) {

    companion object {

        fun enterName() = print("Enter the organization name: ").run { readln() }

        fun enterAddress() = print("Enter the address: ").run { readln() }

        fun enterPhoneNumber(): String {
            val number = print("Enter the number: ").run { readln().trim() }
            return if (validNumber(number)) {
                number
            } else {
                ""
            }
        }
    }

    override fun properties() = listOf("name", "address", "number")

    override fun toString(): String {
        return "Organization name: $name\n" +
                "Address: $address\n" +
                "Number: $number\n" +
                "Time created: ${created.toLocalDateTime(TimeZone.currentSystemDefault()).toString().dropLastWhile { it != ':' }.dropLast(1)}\n" +
                "Time last edit: ${edited.toLocalDateTime(TimeZone.currentSystemDefault()).toString().dropLastWhile { it != ':' }.dropLast(1)}"
    }
}

data class ClassData(val className: String, val properties: MutableMap<String, String>)

class PhoneBook {
    private var contacts = emptyList<Contact>().toMutableList()
    private var file: File? = null

    fun useFile(filename: String) {
        file = File(filename)
        file!!.createNewFile()
        deserialize(file!!.readText())
    }

    fun isEmpty() = contacts.isEmpty()

    fun indices() = contacts.indices

    fun add(contact: Contact): String {
        contacts.add(contact)
        saveToFile()
        return "${if (!Contact.validNumber(contact.number)) "Wrong number format!\n" else ""}The record added."
    }

    fun delete(contact: Contact): String {
        contacts.remove(contact)
        saveToFile()
        return "The record deleted!"
    }

    fun edit(contact: Contact): String {
        var str = "Select a field ("
        for ((fieldName, _) in contact.propertyValues()) {
            str += "$fieldName, "
        }
        val field = print(str.dropLast(2) + "): ").run { readln() }
        if (field in contact.properties()) {
            val value = print("Enter $field: ").run { readln() }
            contact.setProperty(field, value)
            contact.edited = Clock.System.now()
            saveToFile()
            println("Saved")
            return contact.toString()
        } else {
            return "No such field"
        }
    }

    fun search(query: String): PhoneBook {
        val searchResult = PhoneBook()
        for (contact in contacts) {
            for ((_, propertyValue) in contact.propertyValues()) {
                if (Regex(query).matches(propertyValue) || query.lowercase() in propertyValue.lowercase()) {
                    searchResult.add(contact)
                    break
                }
            }
        }
        return searchResult
    }

    fun count(): Int {
        return contacts.size
    }

    fun showInfo(cNum: Int): String {
        return if (cNum - 1 in contacts.indices) contacts[cNum - 1].toString() else "Record $cNum does not exist!"
    }

    fun getRecord(cNum: Int): Contact {
        return contacts[cNum - 1]
    }

    override fun toString(): String {
        var str = ""
        for ((index, contact) in contacts.withIndex()) {
            str += "${index + 1}. ${contact.showName()}\n"
        }
        return str.dropLast(1)
    }

    private fun saveToFile() {
        file?.writeText(serialize())
    }

    private fun serialize(): String {
        var str = "["
        for (contact in contacts) {
            val (key, value) = contact.javaClass.toString().split(" ")
            str += "{\"$key\": \"$value\", "
            str += "\"properties\": ${contact.serialize()}}, "
        }
        return str.dropLast(2) + "]"
    }

    private fun deserialize(jsonString: String) {
        validateJsonFormat(jsonString)
        initializePhoneBook(extractClassInfo(jsonString))
    }

    private fun pListItem(str: String): String {
        val serString = str.trim()
        if (serString.first() == '[') return pList(serString.drop(1))
        if (serString.first() == '{') return pMap(serString.drop(1))
        throw Exception("json file corrupt. List item not a list or map. Item doesn't start with [ or {\n" +
                "String: $serString")
    }

    private fun pList(str: String): String {
        var serString = str.trim()
        if (serString.first() == ']') {
            return serString.drop(1)
        }
        var nextChar: Char
        do {
            serString = pListItem(serString).trim()
            nextChar = serString.first()
            if (nextChar == ',') serString = serString.drop(1).trim()
        } while (nextChar == ',')
        if (nextChar != ']') {
            throw Exception("List doesn't end with ]\nString: $serString")
        }
        return serString.drop(1)
    }

    private fun pMapValue(str: String): String {
        var serString = str.trim()
        if (serString.first() == '{') {
            return pMap(serString.drop(1))
        }
        if (serString.first() == '"') {
            serString = serString.drop(1)
            serString = serString.dropWhile { it != '"' }.drop(1).trim()
        }
        return serString
    }

    private fun pMap(str: String): String {
        var serString = str.trim()
        if (serString.first() == '}') {
            return serString.drop(1)
        }
        var nextChar: Char
        do {
            if (serString.isEmpty() || serString.first() != '"') {
                throw Exception("Not a pMap. \" is missing for the key\nString: $serString")
            }
            serString = serString.drop(1)
            serString = serString.dropWhile { it != '"' }.drop(1).trim()
            if (serString.isEmpty() || serString.first() != ':') {
                throw Exception("Not a pMap. : is missing before the value\nString: $serString")
            }
            serString = serString.drop(1)
            serString = pMapValue(serString).trim()
            nextChar = serString.first()
            if (nextChar == ',') serString = serString.drop(1).trim()
        } while (nextChar == ',')
        if (nextChar != '}') {
            throw Exception("Map doesn't end with }\nString: $serString")
        }
        return serString.drop(1)
    }

    private fun validateJsonFormat(jsonString: String) {
        if (jsonString.isNotEmpty()) {
            if (jsonString.first() == '[') {
                val str = pList(jsonString.drop(1))
                if (str.isNotEmpty()) throw Exception("Incorrect json format (pList)")
            } else if (jsonString.first() == '{') {
                val str = pMap(jsonString.drop(1))
                if (str.isNotEmpty()) throw Exception("Incorrect json format (pMap)")
            } else {
                throw Exception(
                    "Incorrect json format!\nNot a list or a map. String doesn't start with [ or {\n" +
                            "String: $jsonString"
                )
            }
        }
    }

    private fun extractClassInfo(jsonString: String): MutableList<ClassData> {
        val dataList = emptyList<ClassData>().toMutableList()
        if (jsonString.isEmpty()) return dataList
        var str = jsonString.dropWhile { it != '"' }.drop(1)
        do {
            if (str.takeWhile { it != '"' } != "class") {
                throw Exception("ERROR: Wrong format.\n" +
                        "The string \"class\" was expected, found instead the string \"${str.takeWhile { it != '"' }}\""
                )
            }
            str = str.dropWhile { it != '"' }.drop(1).dropWhile { it != '"' }.drop(1)
            val className = str.takeWhile { it != '"' }
            str = str.dropWhile { it != '"' }.drop(1).dropWhile { it != '"' }.drop(1)
            if (str.takeWhile { it != '"' } != "properties") {
                throw Exception("ERROR: Wrong format.\n" +
                        "The string \"properties\" was expected, found instead the string \"${str.takeWhile { it != '"' }}\""
                )
            }
            str = str.dropWhile { it != '"' }.drop(1).dropWhile { it != '"' }
            var nextChar: Char
            val properties = emptyMap<String, String>().toMutableMap()
            do {
                str = str.dropWhile { it != '"' }.drop(1)
                val key = str.takeWhile { it != '"' }
                str = str.dropWhile { it != '"' }.drop(1).dropWhile { it != '"' }.drop(1)
                val value = str.takeWhile { it != '"' }
                str = str.dropWhile { it != '"' }.drop(1).trim()
                properties += key to value
                nextChar = str.first()
                if (nextChar == ',') str = str.dropWhile { it != '"' }
            } while (nextChar == ',')
            str = str.drop(2).trim()
            dataList.add(ClassData(className, properties))
            nextChar = str.first()
            if (nextChar == ',') str = str.dropWhile { it != '"' }.drop(1)
        } while (nextChar == ',')
        return dataList
    }

    private fun initializePhoneBook(classInfo: MutableList<ClassData>) {
        contacts = emptyList<Contact>().toMutableList()
        for (classData in classInfo) {
            when (classData.className.takeLastWhile { it != '.' }) {
                "Person" -> {
                    val person = with(classData.properties) { Person(this["name"]!!, this["surname"]!!, this["birthDate"]!!, this["gender"]!!, this["number"]!!) }
                    person.setProperty("created", classData.properties["created"]!!.toInstant())
                    person.setProperty("edited", classData.properties["edited"]!!.toInstant())
                    contacts.add(person)
                }
                "Organization" -> {
                    val org = with(classData.properties) { Organization(this["name"]!!, this["address"]!!, this["number"]!!) }
                    org.setProperty("created", classData.properties["created"]!!.toInstant())
                    org.setProperty("edited", classData.properties["edited"]!!.toInstant())
                    contacts.add(org)
                }
            }
        }
    }
}