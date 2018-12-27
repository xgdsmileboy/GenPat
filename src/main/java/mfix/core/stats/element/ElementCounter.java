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

    private static HashMap<Pair<String, Integer>, Integer> cacheMap = null;
    private static Integer cacheTotalNumber = null;
    static final String DEFAULT_CACHE_FILE = "/home/renly/MethodTableElements.txt";

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
        Integer countNumber, allNumber = 0;
        if ((cacheMap != null) && (element instanceof MethodElement) && (!queryType.getWithType()) && (queryType._countType == ElementQueryType.CountType.COUNT_FILES)) {
            MethodElement methodElement = (MethodElement)element;

            if (methodElement._elementName == null) {
                throw new ElementException(element.DBKEY_ELEMENT_NAME);
            }
            if (methodElement._argsNumber == null) {
                throw new ElementException(element.DBKEY_ARGS_NUMBER);
            }
            countNumber = cacheMap.getOrDefault(new Pair<String, Integer>(methodElement._elementName, methodElement._argsNumber), 0);
            if (queryType.getWithPercent()) {
                if (cacheTotalNumber == null) {
                    cacheTotalNumber = _connector.query(element.toQueryRowWithoutLimit(queryType));
                }
                allNumber = cacheTotalNumber;
            }
        } else {
            countNumber = _connector.query(element.toQueryRow(queryType));
            allNumber = _connector.query(element.toQueryRowWithoutLimit(queryType));
        }


        if (queryType.getWithPercent()) {
            return allNumber == 0 ? 0 : ((float)countNumber) / allNumber;
        } else {
            return countNumber;
        }
    }

    public void loadCache(String cacheFile) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(cacheFile));
        String line;
        cacheMap = new HashMap<Pair<String, Integer>, Integer>();
        while ((line = br.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(line);
            String elementName = st.nextToken();
            Integer argsNumber = Integer.parseInt(st.nextToken());
            Integer countNumber = Integer.parseInt(st.nextToken());
            cacheMap.put(new Pair<String, Integer>(elementName, argsNumber), countNumber);
        }
    }

    public void loadCache() throws Exception {
        loadCache(DEFAULT_CACHE_FILE);
    }
}
