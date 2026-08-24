package dev.client.api.nullcry.helper.other;

import lombok.experimental.UtilityClass;

import java.security.SecureRandom;
import java.util.List;

@UtilityClass
public class NameGenerator {

    private final SecureRandom random = new SecureRandom();

    private final List<String> SYLLABLES = List.of(
            "a", "e", "i", "o", "u", "y", "ka", "ke", "ki", "ko", "ku", "ky", "sa", "se", "shi", "so", "su", "sy",
            "ta", "te", "chi", "tsu", "to", "ty", "na", "ne", "ni", "no", "nu", "ny", "ha", "he", "hi", "fu", "hu",
            "hy", "ma", "me", "mi", "mo", "mu", "my", "ya", "yu", "yo", "ra", "re", "ri", "ro", "ru", "ry", "wa",
            "wo", "n", "zi", "zo", "zu", "je", "ji", "fa", "fe", "fi", "fo", "fu", "ga", "ge", "gi", "go", "gu",
            "la", "le", "li", "lo", "lu", "ba", "be", "bi", "bo", "bu", "da", "de", "di", "do", "du", "xa", "xe",
            "xi", "xo", "xu", "xdead", "exc", "sex", "lol", "xx", "for", "ne", "top", "gg"
    );

    public String generate() {
        int maxLength = random.nextInt(8) + 8;
        StringBuilder nickName = new StringBuilder(maxLength);

        while (nickName.length() < maxLength) {
            String syllable = SYLLABLES.get(random.nextInt(SYLLABLES.size()));
            if (nickName.length() + syllable.length() <= maxLength) {
                nickName.append(syllable);
            }
        }

        applyRandomCapitalization(nickName);

        if (random.nextInt(10) == 0 && nickName.length() < maxLength - 2) {
            nickName.append(random.nextInt(100));
        }

        return nickName.toString();
    }

    private void applyRandomCapitalization(StringBuilder nickName) {
        int numberOfChanges = random.nextInt(nickName.length() / 2 + 1);
        for (int i = 0; i < numberOfChanges; i++) {
            int index = random.nextInt(nickName.length());
            nickName.setCharAt(index, Character.toUpperCase(nickName.charAt(index)));
        }
    }
}