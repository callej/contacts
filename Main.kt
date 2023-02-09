package contacts

data class Result(val state: State, val list: PhoneBook)

enum class State {
    START,
    MENU,
    LIST,
    SEARCH,
    RECORD,
    STOP;
}

enum class Menu(val show: String, val match: String, val command: (State, PhoneBook) -> Result, val nextState: State) {
    ADD("add", "add", { state, phoneBook -> cmdAdd(state, phoneBook) }, State.MENU),
    LIST("list", "list", { state, phoneBook -> cmdList(state, phoneBook) }, State.LIST),
    SEARCH("search", "search", { state, phoneBook -> cmdSearch(state, phoneBook) }, State.SEARCH),
    COUNT("count", "count", { state, phoneBook -> cmdCount(state, phoneBook) }, State.MENU),
    EXIT("exit", "exit", { state, phoneBook -> cmdExit(state, phoneBook) }, State.STOP),
    NO_MATCH("", "", { state, phoneBook -> noAction(state, phoneBook) }, State.MENU);

    companion object {
        fun task(action: String): Menu {
            for (menuItem in values()) {
                if (menuItem.match.toRegex().matches(action)) return menuItem
            }
            return NO_MATCH
        }

        override fun toString(): String {
            var menu = "[menu] Enter action ("
            for (menuItem in values()) {
                menu += "${menuItem.show}, "
            }
            return menu.dropLast(4) + "): "
        }
    }
}

enum class CList(val show: String, val match: String, val command: (State, State, PhoneBook, PhoneBook, String) -> Result, val nextState: State) {
    NUMBER("[number]", "\\d+", { origState, nextState, list, _, action -> cmdShowRecord(origState, nextState, list, action) }, State.RECORD),
    BACK("back", "back", { _, state, list, _, _ -> noAction(state, list) }, State.MENU),
    NO_MATCH("", "", { _, state, list, _, _ -> noAction(state, list) }, State.LIST);

    companion object {
        fun task(action: String): CList {
            for (listItem in values()) {
                if (listItem.match.toRegex().matches(action)) return listItem
            }
            return NO_MATCH
        }

        override fun toString(): String {
            var list = "[list] Enter action ("
            for (listItem in values()) {
                list += "${listItem.show}, "
            }
            return list.dropLast(4) + "): "
        }
    }
}

enum class Search(val show: String, val match: String, val command: (State, State, PhoneBook, PhoneBook, String) -> Result, val nextState: State) {
    NUMBER("[number]", "\\d+", { origState, nextState, list, _, action -> cmdShowRecord(origState, nextState, list, action) }, State.RECORD),
    BACK("back", "back", { _, state, list, _, _ -> noAction(state, list) }, State.MENU),
    AGAIN("again", "again", { _, state, _, phoneBook, _ -> cmdSearch(state, phoneBook) }, State.SEARCH),
    NO_MATCH("", "", { _, state, list, _, _ -> noAction(state, list) }, State.SEARCH);

    companion object {
        fun task(action: String): Search {
            for (searchItem in values()) {
                if (searchItem.match.toRegex().matches(action)) return searchItem
            }
            return NO_MATCH
        }

        override fun toString(): String {
            var search = "[search] Enter action ("
            for (searchItem in values()) {
                search += "${searchItem.show}, "
            }
            return search.dropLast(4) + "): "
        }
    }
}

enum class Record(val show: String, val match: String, val command: (State, PhoneBook, PhoneBook) -> Result, val nextState: State) {
    EDIT("edit", "edit", { state, list, phoneBook -> cmdEdit(state, list, phoneBook) }, State.RECORD),
    DELETE("delete", "delete", { state, list, phoneBook -> cmdDelete(state, list, phoneBook) }, State.MENU),
    MENU("menu", "menu", { state, list, _ -> noAction(state, list) }, State.MENU),
    NO_MATCH("", "", { state, list, _ -> noAction(state, list) }, State.RECORD);

    companion object {
        fun task(action: String): Record {
            for (RecordItem in values()) {
                if (RecordItem.match.toRegex().matches(action)) return RecordItem
            }
            return NO_MATCH
        }

        override fun toString(): String {
            var record = "[record] Enter action ("
            for (recordItem in values()) {
                record += "${recordItem.show}, "
            }
            return record.dropLast(4) + "): "
        }
    }
}

fun noAction(state: State, phoneBook: PhoneBook) = Result(state, phoneBook)

fun addPerson(phoneBook: PhoneBook) {
    val name = Person.enterName()
    val surname = Person.enterSurname()
    val birthDate = Person.enterBirthDate()
    val gender = Person.enterGender()
    val number = Person.enterPhoneNumber()
    println(phoneBook.add(Person(name, surname, birthDate, gender, number)))
}

fun addOrganization(phoneBook: PhoneBook) {
    val name = Organization.enterName()
    val address = Organization.enterAddress()
    val number = Organization.enterPhoneNumber()
    println(phoneBook.add(Organization(name, address, number)))
}

fun cmdAdd(state: State, phoneBook: PhoneBook): Result {
    when (print("Enter the type (person, organization): ").run { readln() }) {
        "person" -> addPerson(phoneBook)
        "organization" -> addOrganization(phoneBook)
    }
    return Result(state, PhoneBook())
}

fun cmdList(state: State, phoneBook: PhoneBook): Result {
    if (phoneBook.isEmpty()) {
        println("No records to show!")
        return Result(State.MENU, phoneBook)
    } else {
        println(phoneBook)
        return Result(state, phoneBook)
    }
}

fun cmdSearch(state: State, phoneBook: PhoneBook): Result {
    if (phoneBook.isEmpty()) {
        println("No records to search!")
        return Result(State.MENU, phoneBook)
    }
    val query = print("Enter search query: ").run { readln() }
    val matchingRecords = phoneBook.search(query)
    println(matchingRecords)
    return Result(state, matchingRecords)
}

fun cmdShowRecord(origState: State, nextState: State, list: PhoneBook, action: String): Result {
    println(list.showInfo(action.toInt()))
    if (action.toInt() - 1 in list.indices()) {
        val selected = list.getRecord(action.toInt())
        val newList = PhoneBook()
        newList.add(selected)
        return Result(nextState, newList)
    }
    return Result(origState, list)
}

fun cmdDelete(state: State, list: PhoneBook, phoneBook: PhoneBook): Result {
    println(phoneBook.delete(list.getRecord(1)))
    return Result(state, phoneBook)
}

fun cmdEdit(state: State, list: PhoneBook, phoneBook: PhoneBook): Result {
    println(phoneBook.edit(list.getRecord(1)))
    return Result(state, phoneBook)
}

fun cmdCount(state: State, phoneBook: PhoneBook): Result {
    println("The Phone Book has ${phoneBook.count()} record${if (phoneBook.count() != 1) "s" else ""}.")
    return Result(state, PhoneBook())
}

fun cmdExit(state: State, phoneBook: PhoneBook) = Result(state, phoneBook)

fun init(phoneBook: PhoneBook, args: Array<String>): State {
    if (args.isNotEmpty()) {
        println("open ${args.first()}")
        try {
            phoneBook.useFile(args.first())
        } catch (e: Exception) {
            println(e.message)
        }
    }
    return State.MENU
}

fun main(args: Array<String>) {
    val phoneBook = PhoneBook()
    var inProcessList = PhoneBook()
    var state = State.START
    while (true) {
        when (state) {
            State.START -> state = init(phoneBook, args)
            State.MENU -> {
                val task = Menu.task(print(Menu).run { readln() })
                val (st, list) = task.command(task.nextState, phoneBook)
                state = st
                inProcessList = list
            }
            State.LIST -> {
                val action = print(CList).run { readln() }
                val task = CList.task(action)
                val (st, list) = task.command(State.LIST, task.nextState, inProcessList, phoneBook, action)
                state = st
                inProcessList = list
            }
            State.SEARCH -> {
                val action = print(Search).run { readln() }
                val task = Search.task(action)
                val (st, list) = task.command(State.SEARCH, task.nextState, inProcessList, phoneBook, action)
                state = st
                inProcessList = list
            }
            State.RECORD -> {
                val task = Record.task(print(Record).run { readln() })
                val (st, list) = task.command(task.nextState, inProcessList, phoneBook)
                state = st
                inProcessList = list
            }
            State.STOP -> break
        }
        println()
    }
}