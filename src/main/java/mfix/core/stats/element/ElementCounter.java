package mfix.core.stats.element;

import mfix.common.util.Pair;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.StringTokenizer;

/**
 * @author: Luyao Ren
 * @date: 2018/12/09
 */
public class ElementCounter {
    private DatabaseConnector _connector = null;
    static final String cacheUnloadedErrorMessage = "CacheSource";
    static final String cacheUnsupportErrorMessage = "Corresponding CacheSource";

    private static HashMap<Pair<String, Integer>, Integer> cacheMap = null;
    private static Integer cacheMaptotalNumber;

    public void open() {
        _connector = new DatabaseConnector();
        _connector.open();
    }

    public void close() {
        _connector.close();
    }

    public void add(Element element) throws ElementException {
        _connector.add(element.toInsertRow());
    }

    public float count(Element element, ElementQueryType queryType) throws ElementException {
        Integer countNumber = _connector.query(element.toQueryRow(queryType));

        if (queryType.getWithPercent()) {
            Integer allNumber = _connector.query(element.toQueryRowWithoutLimit(queryType));
            return allNumber == 0 ? 0 : ((float)countNumber) / allNumber;
        } else {
            return countNumber;
        }
    }

    public void loadCache(String cacheFile) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(cacheFile));
            String line;
            cacheMap = new HashMap<Pair<String, Integer>, Integer>();
            cacheMaptotalNumber = 0;
            while ((line = br.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line);
                String elementName = st.nextToken();
                Integer argsNumber = Integer.parseInt(st.nextToken());
                Integer countNumber = Integer.parseInt(st.nextToken());
                cacheMap.put(new Pair<String, Integer>(elementName, argsNumber), countNumber);
                cacheMaptotalNumber += countNumber;
            }
        } catch (Exception e) {
            e.printStackTrace();
            cacheMap = null;
        }
    }

    public float countOnCache(MethodElement element, ElementQueryType queryType) throws ElementException {
        if (cacheMap == null) {
            throw new ElementException(cacheUnloadedErrorMessage);
        }
        if (element._elementName == null) {
            throw new ElementException(element.DBKEY_ELEMENT_NAME);
        }
        if (element._argsNumber == null) {
            throw new ElementException(element.DBKEY_ARGS_NUMBER);
        }
        if ((!queryType.getWithType()) && (queryType._countType == ElementQueryType.CountType.COUNT_FILES)) {
            float value = (float)cacheMap.getOrDefault(new Pair<String, Integer>(element._elementName, element._argsNumber), 0);
            if (queryType.getWithPercent()) {
                value = value / cacheMaptotalNumber;
            }
            return value;
        } else {
            throw new ElementException(cacheUnsupportErrorMessage);
        }
    }
}
