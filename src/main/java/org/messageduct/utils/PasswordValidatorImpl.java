package org.messageduct.utils;

import org.flowutils.Check;

import java.util.*;

/**
 * Password validator with minimum password length requirement, a default dictionary of non-permitted common passwords,
 * check against username as password, and check against password being just one character.
 */
// TODO: We could add checks for having characters from different groups as well.
// TODO: We could check that the password has more than N different characters as well
// TODO: We could maybe do a password strength calculator as well
// TODO: Could maybe load a dictionary from a file too, but at that point it might be easier to just use a cracking library to test.
public final class PasswordValidatorImpl implements PasswordValidator {

    public static final int DEFAULT_MIN_PASSWORD_LENGTH = 12;
    public static final int DEFAULT_MAX_PASSWORD_LENGTH = 256;
    public static final int DEFAULT_MIN_NUMBER_OF_DIFFERENT_CHARACTER_TYPES = 1;

    /**
     * The thousand most common passwords or so (they seem to be 8 or less characters long).
     */
    private static final List<String> DEFAULT_DICTIONARY = Arrays.asList(
            "password", "123456", "12345678", "1234", "qwerty", "12345", "dragon", "pussy", "baseball", "football",
            "letmein", "monkey", "696969", "abc123", "mustang", "michael", "shadow", "master", "jennifer", "111111",
            "2000", "jordan", "superman", "harley", "1234567", "fuckme", "hunter", "fuckyou", "trustno1", "ranger",
            "buster", "thomas", "tigger", "robert", "soccer", "fuck", "batman", "test", "pass", "killer", "hockey",
            "george", "charlie", "andrew", "michelle", "love", "sunshine", "jessica", "asshole", "6969", "pepper",
            "daniel", "access", "123456789", "654321", "joshua", "maggie", "starwars", "silver", "william", "dallas",
            "yankees", "123123", "ashley", "666666", "hello", "amanda", "orange", "biteme", "freedom", "computer",
            "sexy", "thunder", "nicole", "ginger", "heather", "hammer", "summer", "corvette", "taylor", "fucker",
            "austin", "1111", "merlin", "matthew", "121212", "golfer", "cheese", "princess", "martin", "chelsea",
            "patrick", "richard", "diamond", "yellow", "bigdog", "secret", "asdfgh", "sparky", "cowboy", "camaro",
            "anthony", "matrix", "falcon", "iloveyou", "bailey", "guitar", "jackson", "purple", "scooter", "phoenix",
            "aaaaaa", "morgan", "tigers", "porsche", "mickey", "maverick", "cookie", "nascar", "peanut", "justin",
            "131313", "money", "horny", "samantha", "panties", "steelers", "joseph", "snoopy", "boomer", "whatever",
            "iceman", "smokey", "gateway", "dakota", "cowboys", "eagles", "chicken", "dick", "black", "zxcvbn",
            "please", "andrea", "ferrari", "knight", "hardcore", "melissa", "compaq", "coffee", "booboo", "bitch",
            "johnny", "bulldog", "xxxxxx", "welcome", "james", "player", "ncc1701", "wizard", "scooby", "charles",
            "junior", "internet", "bigdick", "mike", "brandy", "tennis", "blowjob", "banana", "monster", "spider",
            "lakers", "miller", "rabbit", "enter", "mercedes", "brandon", "steven", "fender", "john", "yamaha",
            "diablo", "chris", "boston", "tiger", "marine", "chicago", "rangers", "gandalf", "winter", "bigtits",
            "barney", "edward", "raiders", "porn", "badboy", "blowme", "spanky", "bigdaddy", "johnson", "chester",
            "london", "midnight", "blue", "fishing", "0", "hannah", "slayer", "11111111", "rachel", "sexsex", "redsox",
            "thx1138", "asdf", "marlboro", "panther", "zxcvbnm", "arsenal", "oliver", "qazwsx", "mother", "victoria",
            "7777777", "jasper", "angel", "david", "winner", "crystal", "golden", "butthead", "viking", "jack",
            "iwantu", "shannon", "murphy", "angels", "prince", "cameron", "girls", "madison", "wilson", "carlos",
            "hooters", "willie", "startrek", "captain", "maddog", "jasmine", "butter", "booger", "angela", "golf",
            "lauren", "rocket", "tiffany", "theman", "dennis", "liverpoo", "flower", "forever", "green", "jackie",
            "muffin", "turtle", "sophie", "danielle", "redskins", "toyota", "jason", "sierra", "winston", "debbie",
            "giants", "packers", "newyork", "jeremy", "casper", "bubba", "112233", "sandra", "lovers", "mountain",
            "united", "cooper", "driver", "tucker", "helpme", "fucking", "pookie", "lucky", "maxwell", "8675309",
            "bear", "suckit", "gators", "5150", "222222", "shithead", "fuckoff", "jaguar", "monica", "fred", "happy",
            "hotdog", "tits", "gemini", "lover", "xxxxxxxx", "777777", "canada", "nathan", "victor", "florida",
            "88888888", "nicholas", "rosebud", "metallic", "doctor", "trouble", "success", "stupid", "tomcat",
            "warrior", "peaches", "apples", "fish", "qwertyui", "magic", "buddy", "dolphins", "rainbow", "gunner",
            "987654", "freddy", "alexis", "braves", "cock", "2112", "1212", "cocacola", "xavier", "dolphin", "testing",
            "bond007", "member", "calvin", "voodoo", "7777", "samson", "alex", "apollo", "fire", "tester", "walter",
            "beavis", "voyager", "peter", "porno", "bonnie", "rush2112", "beer", "apple", "scorpio", "jonathan",
            "skippy", "sydney", "scott", "red123", "power", "gordon", "travis", "beaver", "star", "jackass", "flyers",
            "boobs", "232323", "zzzzzz", "steve", "rebecca", "scorpion", "doggie", "legend", "ou812", "yankee",
            "blazer", "bill", "runner", "birdie", "bitches", "555555", "parker", "topgun", "asdfasdf", "heaven",
            "viper", "animal", "2222", "bigboy", "4444", "arthur", "baby", "private", "godzilla", "donald", "williams",
            "lifehack", "phantom", "dave", "rock", "august", "sammy", "cool", "brian", "platinum", "jake", "bronco",
            "paul", "mark", "frank", "heka6w2", "copper", "billy", "cumshot", "garfield", "willow", "cunt", "little",
            "carter", "slut", "albert", "69696969", "kitten", "super", "jordan23", "eagle1", "shelby", "america",
            "11111", "jessie", "house", "free", "123321", "chevy", "bullshit", "white", "broncos", "horney", "surfer",
            "nissan", "999999", "saturn", "airborne", "elephant", "marvin", "shit", "action", "adidas", "qwert",
            "kevin", "1313", "explorer", "walker", "police", "christin", "december", "benjamin", "wolf", "sweet",
            "therock", "king", "online", "dickhead", "brooklyn", "teresa", "cricket", "sharon", "dexter", "racing",
            "penis", "gregory", "0", "teens", "redwings", "dreams", "michigan", "hentai", "magnum", "87654321",
            "nothing", "donkey", "trinity", "digital", "333333", "stella", "cartman", "guinness", "123abc", "speedy",
            "buffalo", "kitty", "pimpin", "eagle", "einstein", "kelly", "nelson", "nirvana", "vampire", "xxxx",
            "playboy", "louise", "pumpkin", "snowball", "test123", "girl", "sucker", "mexico", "beatles", "fantasy",
            "ford", "gibson", "celtic", "marcus", "cherry", "cassie", "888888", "natasha", "sniper", "chance",
            "genesis", "hotrod", "reddog", "alexande", "college", "jester", "passw0rd", "bigcock", "smith", "lasvegas",
            "carmen", "slipknot", "3333", "death", "kimberly", "1q2w3e", "eclipse", "1q2w3e4r", "stanley", "samuel",
            "drummer", "homer", "montana", "music", "aaaa", "spencer", "jimmy", "carolina", "colorado", "creative",
            "hello1", "rocky", "goober", "friday", "bollocks", "scotty", "abcdef", "bubbles", "hawaii", "fluffy",
            "mine", "stephen", "horses", "thumper", "5555", "pussies", "darkness", "asdfghjk", "pamela", "boobies",
            "buddha", "vanessa", "sandman", "naughty", "douglas", "honda", "matt", "azerty", "6666", "shorty", "money1",
            "beach", "loveme", "4321", "simple", "poohbear", "444444", "badass", "destiny", "sarah", "denise",
            "vikings", "lizard", "melanie", "assman", "sabrina", "nintendo", "water", "good", "howard", "time",
            "123qwe", "november", "xxxxx", "october", "leather", "bastard", "young", "101010", "extreme", "hard",
            "password1", "vincent", "pussy1", "lacrosse", "hotmail", "spooky", "amateur", "alaska", "badger",
            "paradise", "maryjane", "poop", "crazy", "mozart", "video", "russell", "vagina", "spitfire", "anderson",
            "norman", "eric", "cherokee", "cougar", "barbara", "long", "420420", "family", "horse", "enigma", "allison",
            "raider", "brazil", "blonde", "jones", "55555", "dude", "drowssap", "jeff", "school", "marshall", "lovely",
            "1qaz2wsx", "jeffrey", "caroline", "franklin", "booty", "molly", "snickers", "leslie", "nipples",
            "courtney", "diesel", "rocks", "eminem", "westside", "suzuki", "daddy", "passion", "hummer", "ladies",
            "zachary", "frankie", "elvis", "reggie", "alpha", "suckme", "simpson", "patricia", "147147", "pirate",
            "tommy", "semperfi", "jupiter", "redrum", "freeuser", "wanker", "stinky", "ducati", "paris", "natalie",
            "babygirl", "bishop", "windows", "spirit", "pantera", "monday", "patches", "brutus", "houston", "smooth",
            "penguin", "marley", "forest", "cream", "212121", "flash", "maximus", "nipple", "bobby", "bradley",
            "vision", "pokemon", "champion", "fireman", "indian", "softball", "picard", "system", "clinton", "cobra",
            "enjoy", "lucky1", "claire", "claudia", "boogie", "timothy", "marines", "security", "dirty", "admin",
            "wildcats", "pimp", "dancer", "hardon", "veronica", "fucked", "abcd1234", "abcdefg", "ironman", "wolverin",
            "remember", "great", "freepass", "bigred", "squirt", "justice", "francis", "hobbes", "kermit", "pearljam",
            "mercury", "domino", "9999", "denver", "brooke", "rascal", "hitman", "mistress", "simon", "tony", "bbbbbb",
            "friend", "peekaboo", "naked", "budlight", "electric", "sluts", "stargate", "saints", "bondage", "brittany",
            "bigman", "zombie", "swimming", "duke", "qwerty1", "babes", "scotland", "disney", "rooster", "brenda",
            "mookie", "swordfis", "candy", "duncan", "olivia", "hunting", "blink182", "alicia", "8888", "samsung",
            "bubba1", "whore", "virginia", "general", "passport", "aaaaaaaa", "erotic", "liberty", "arizona", "jesus",
            "abcd", "newport", "skipper", "rolltide", "balls", "happy1", "galore", "christ", "weasel", "242424",
            "wombat", "digger", "classic", "bulldogs", "poopoo", "accord", "popcorn", "turkey", "jenny", "amber",
            "bunny", "mouse", "7007", "titanic", "liverpool", "dreamer", "everton", "friends", "chevelle", "carrie",
            "gabriel", "psycho", "nemesis", "burton", "pontiac", "connor", "eatme", "lickme", "roland", "cumming",
            "mitchell", "ireland", "lincoln", "arnold", "spiderma", "patriots", "goblue", "devils", "eugene", "empire",
            "asdfg", "cardinal", "brown", "shaggy", "froggy", "qwer", "kawasaki", "kodiak", "people", "phpbb", "light",
            "54321", "kramer", "chopper", "hooker", "honey", "whynot", "lesbian", "lisa", "baxter", "adam", "snake",
            "teen", "ncc1701d", "qqqqqq", "airplane", "britney", "avalon", "sandy", "sugar", "sublime", "stewart",
            "wildcat", "raven", "scarface", "elizabet", "123654", "trucks", "wolfpack", "pervert", "lawrence",
            "raymond", "redhead", "american", "alyssa", "bambam", "movie", "woody", "shaved", "snowman", "tiger1",
            "chicks", "raptor", "1969", "stingray", "shooter", "france", "stars", "madmax", "kristen", "sports",
            "jerry", "789456", "garcia", "simpsons", "lights", "ryan", "looking", "chronic", "alison", "hahaha",
            "packard", "hendrix", "perfect", "service", "spring", "srinivas", "spike", "katie", "252525", "oscar",
            "brother", "bigmac", "suck", "single", "cannon", "georgia", "popeye", "tattoo", "texas", "party", "bullet",
            "taurus", "sailor", "wolves", "panthers", "japan", "strike", "flowers", "pussycat", "chris1", "loverboy",
            "berlin", "sticky", "marina", "tarheels", "fisher", "russia", "connie", "wolfgang", "testtest", "mature",
            "bass", "catch22", "juice", "michael1", "nigger", "159753", "women", "alpha1", "trooper", "hawkeye", "head",
            "freaky", "dodgers", "pakistan", "machine", "pyramid", "vegeta", "katana", "moose", "tinker", "coyote",
            "infinity", "inside", "pepsi", "letmein1", "bang", "control", "hercules", "morris", "james1", "tickle",
            "outlaw", "browns", "billybob", "pickle", "test1", "michele", "antonio", "sucks", "pavilion", "changeme"
    );

    private final List<String> userSpecifiedDictionary = new ArrayList<String>();
    private final int minLength;
    private final int maxLength;

    /**
     * Creates a new PasswordValidatorImpl with a default dictionary and default password length requirements.
     */
    public PasswordValidatorImpl() {
        this(DEFAULT_MIN_PASSWORD_LENGTH);
    }

    /**
     * Creates a new PasswordValidatorImpl with a default dictionary and the specified password min length.
     *
     * @param minLength minimum password length.
     */
    public PasswordValidatorImpl(int minLength) {
        this(minLength, DEFAULT_MAX_PASSWORD_LENGTH);
    }

    /**
     * Creates a new PasswordValidatorImpl with a default dictionary and the specified password length requirements.
     *
     * @param minLength minimum password length.
     * @param maxLength maximum password length.
     */
    public PasswordValidatorImpl(int minLength, int maxLength) {
        this(minLength, maxLength, null);
    }

    /**
     * Creates a new PasswordValidatorImpl with a default dictionary in addition to the specified dictionary.
     *
     * @param minLength minimum password length.
     * @param maxLength maximum password length.
     * @param dictionaryWords words to add to the dictionary of non-permitted passwords, in addition to the default ones.
     */
    public PasswordValidatorImpl(int minLength, int maxLength, Collection<String> dictionaryWords) {
        this.minLength = minLength;
        this.maxLength = maxLength;

        // Add user specified dictionary
        if (dictionaryWords != null) addDictionaryWords(dictionaryWords);
    }

    /**
     * Adds a word to the dictionary of non-permitted passwords.
     */
    public void addDictionaryWord(String dictionaryWord) {
        Check.notNull(dictionaryWord, "dictionaryWord");
        userSpecifiedDictionary.add(dictionaryWord);
    }

    /**
     * Adds words to the dictionary of non-permitted passwords.
     */
    public void addDictionaryWords(Collection<String> dictionaryWords) {
        userSpecifiedDictionary.addAll(dictionaryWords);
    }

    /**
     * Adds words to the dictionary of non-permitted passwords.
     */
    public void addDictionaryWords(String ... dictionaryWords) {
        Collections.addAll(userSpecifiedDictionary, dictionaryWords);
    }

    @Override public String check(char[] password, String userName) {
        Check.notNull(password, "password");

        // Check length
        if (password.length < minLength) return "The password is too short, minimum length is " + minLength + ".";
        if (password.length > maxLength) return "The password is too long, maximum length is " + maxLength + " (sorry about that).";

        // Check against username
        if (equalsIgnoreCase(userName, password)) return "Password and username can not be the same.";

        // Check that not all the same character
        if (containsOnlyOneChar(password)) return "The password can not be all the same character.";

        // Check against dictionary of known bad passwords / dictionary words
        if (containedInDictionary(password, userSpecifiedDictionary)) return "The password is not acceptable because it matches a known common password or dictionary word";
        if (containedInDictionary(password, DEFAULT_DICTIONARY)) return "The password is not acceptable because it matches a known common password or dictionary word";

        return null;
    }

    private boolean containedInDictionary(char[] password, final List<String> dictionary) {
        final int dictionarySize = dictionary.size();
        for (int i = 0; i < dictionarySize; i++) {
            if (equalsIgnoreCase(dictionary.get(i), password)) return true;
        }
        return false;
    }

    private boolean containsOnlyOneChar(char[] s) {
        char c0 = s[0];
        for (char c : s) {
            if (c != c0) return false;
        }

        return true;
    }

    private boolean equalsIgnoreCase(String s1, char[] s2) {
        if (s1.length() != s2.length) return false;

        for (int i = 0; i < s2.length; i++) {
            if (Character.toLowerCase(s1.charAt(i)) !=
                Character.toLowerCase(s2[i])) return false;
        }

        return true;
    }
}
