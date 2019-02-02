package mfix.core.stats.element;

import mfix.common.util.Constant;
import mfix.common.util.LevelLogger;
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

    private static HashMap<Pair<String, Integer>, Integer> cacheMapForAPI = null;
    private static HashMap<Pair<Pair<String, Integer>, String>, Integer> cacheMapForAPIWithType = null;

    private static Integer cacheTotalNumber = null;

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
        if ((cacheMapForAPI != null) && (element instanceof MethodElement) && (!queryType.getWithType()) && (queryType._countType == ElementQueryType.CountType.COUNT_FILES)) {
            MethodElement methodElement = (MethodElement)element;

            if (methodElement._elementName == null) {
                throw new ElementException(element.DBKEY_ELEMENT_NAME);
            }
            if (methodElement._argsNumber == null) {
                throw new ElementException(element.DBKEY_ARGS_NUMBER);
            }
            countNumber = cacheMapForAPI.getOrDefault(new Pair<String, Integer>(methodElement._elementName, methodElement._argsNumber), 0);
            if (queryType.getWithPercent()) {
                if (cacheTotalNumber == null) {
                    cacheTotalNumber = _connector.query(element.toQueryRowWithoutLimit(queryType));
                }
                allNumber = cacheTotalNumber;
            }
        } else if ((cacheMapForAPIWithType != null) && (element instanceof MethodElement) && (queryType.getWithType()) && (queryType._countType == ElementQueryType.CountType.COUNT_FILES)) {
            MethodElement methodElement = (MethodElement)element;

            if (methodElement._elementName == null) {
                throw new ElementException(element.DBKEY_ELEMENT_NAME);
            }
            if (methodElement._argsNumber == null) {
                throw new ElementException(element.DBKEY_ARGS_NUMBER);
            }
            if (methodElement._objType == null) {
                throw new ElementException(element.DBKEY_OBJ_TYPE);
            }

            countNumber = cacheMapForAPIWithType.getOrDefault(new Pair<>(new Pair<>(methodElement._elementName, methodElement._argsNumber), methodElement._objType), 0);
            if (queryType.getWithPercent()) {
                if (cacheTotalNumber == null) {
                    cacheTotalNumber = _connector.query(element.toQueryRowWithoutLimit(queryType));
                }
                allNumber = cacheTotalNumber;
            }
        } else {
            LevelLogger.error("[ERROR] Query on Database.");
            countNumber = _connector.query(element.toQueryRow(queryType));
            allNumber = _connector.query(element.toQueryRowWithoutLimit(queryType));
        }


        if (queryType.getWithPercent()) {
            return allNumber == 0 ? 0 : ((float)countNumber) / allNumber;
        } else {
            return countNumber;
        }
    }

    public void loadCacheWithoutType(String cacheFile) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(cacheFile));
        String line;
        cacheMapForAPI = new HashMap<>();
        while ((line = br.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(line);
            String elementName = st.nextToken();
            Integer argsNumber = Integer.parseInt(st.nextToken());
            Integer countNumber = Integer.parseInt(st.nextToken());
            cacheMapForAPI.put(new Pair<String, Integer>(elementName, argsNumber), countNumber);
        }
    }

    public void loadCacheWithType(String cacheFile) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(cacheFile));
        String line;
        cacheMapForAPIWithType = new HashMap<>();
        while ((line = br.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(line);
            String elementName = st.nextToken();
            Integer argsNumber = Integer.parseInt(st.nextToken());
            String objType = st.nextToken();
            Integer countNumber = Integer.parseInt(st.nextToken());
            cacheMapForAPIWithType.put(new Pair<>(new Pair<>(elementName, argsNumber), objType), countNumber);
        }
    }

    public void loadCache() {
        try {
            loadCacheWithoutType(Constant.DB_CACHE_FILE);
            loadCacheWithType(Constant.DB_CACHE_FILE_WITH_TYPE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
