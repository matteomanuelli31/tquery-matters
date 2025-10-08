include "interface_tags.iol"
from @jolie.tquery.main import TQuery
include "console.iol"

service ServerTags {
    embed TQuery as TQuery

    execution { concurrent }

    inputPort ip {
        Location: "socket://localhost:9000"
        Protocol: http {
            .format = "json"
        }
        Interfaces: TagNormalizationInterface
    }

    main {
        normalizeTags(request)(response) {
            response.name = request.name;

            // ========================================================
            // WITHOUT TQUERY: Multiple checks
            // ========================================================
            if (is_defined(request.tags)) {
                if (#request.tags == 1) {
                    output1.tags._ << request.tags
                } else {
                    output1.tags << request.tags
                }
            };

            // ========================================================
            // WITH TQUERY: Single unwind handles all cases
            // ========================================================
            unwind@TQuery({
                data << request
                query = "tags"
            })(unwound);

            for (i = 0, i < #unwound.result, i++) {
                response.normalizedTags[i] = unwound.result[i].tags
            }
        }
    }
}
