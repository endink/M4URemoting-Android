package com.labijie.m4u


internal object FaceLandmarksConnections {
    val FACE_LANDMARKS_LIPS : Set<Array<Int>> =  setOf(
        Connection.create(61, 146),
        Connection.create(146, 91),
        Connection.create(91, 181),
        Connection.create(181, 84),
        Connection.create(84, 17),
        Connection.create(17, 314),
        Connection.create(314, 405),
        Connection.create(405, 321),
        Connection.create(321, 375),
        Connection.create(375, 291),
        Connection.create(61, 185),
        Connection.create(185, 40),
        Connection.create(40, 39),
        Connection.create(39, 37),
        Connection.create(37, 0),
        Connection.create(0, 267),
        Connection.create(267, 269),
        Connection.create(269, 270),
        Connection.create(270, 409),
        Connection.create(409, 291),
        Connection.create(78, 95),
        Connection.create(95, 88),
        Connection.create(88, 178),
        Connection.create(178, 87),
        Connection.create(87, 14),
        Connection.create(14, 317),
        Connection.create(317, 402),
        Connection.create(402, 318),
        Connection.create(318, 324),
        Connection.create(324, 308),
        Connection.create(78, 191),
        Connection.create(191, 80),
        Connection.create(80, 81),
        Connection.create(81, 82),
        Connection.create(82, 13),
        Connection.create(13, 312),
        Connection.create(312, 311),
        Connection.create(311, 310),
        Connection.create(310, 415),
        Connection.create(415, 308)
    )
    val FACE_LANDMARKS_LEFT_EYE : Set<Array<Int>> =  setOf(
        Connection.create(263, 249),
        Connection.create(249, 390),
        Connection.create(390, 373),
        Connection.create(373, 374),
        Connection.create(374, 380),
        Connection.create(380, 381),
        Connection.create(381, 382),
        Connection.create(382, 362),
        Connection.create(263, 466),
        Connection.create(466, 388),
        Connection.create(388, 387),
        Connection.create(387, 386),
        Connection.create(386, 385),
        Connection.create(385, 384),
        Connection.create(384, 398),
        Connection.create(398, 362)
    )
    val FACE_LANDMARKS_LEFT_EYE_BROW: Set<Array<Int>> = setOf(
        Connection.create(276, 283),
        Connection.create(283, 282),
        Connection.create(282, 295),
        Connection.create(295, 285),
        Connection.create(300, 293),
        Connection.create(293, 334),
        Connection.create(334, 296),
        Connection.create(296, 336)
    )
    val FACE_LANDMARKS_LEFT_IRIS: Set<Array<Int>> = setOf(
        Connection.create(474, 475),
        Connection.create(475, 476),
        Connection.create(476, 477),
        Connection.create(477, 474)
    )
    val FACE_LANDMARKS_RIGHT_EYE: Set<Array<Int>> = setOf(
        Connection.create(33, 7),
        Connection.create(7, 163),
        Connection.create(163, 144),
        Connection.create(144, 145),
        Connection.create(145, 153),
        Connection.create(153, 154),
        Connection.create(154, 155),
        Connection.create(155, 133),
        Connection.create(33, 246),
        Connection.create(246, 161),
        Connection.create(161, 160),
        Connection.create(160, 159),
        Connection.create(159, 158),
        Connection.create(158, 157),
        Connection.create(157, 173),
        Connection.create(173, 133)
    )
    val FACE_LANDMARKS_RIGHT_EYE_BROW: Set<Array<Int>> = setOf(
        Connection.create(46, 53),
        Connection.create(53, 52),
        Connection.create(52, 65),
        Connection.create(65, 55),
        Connection.create(70, 63),
        Connection.create(63, 105),
        Connection.create(105, 66),
        Connection.create(66, 107)
    )
    val FACE_LANDMARKS_RIGHT_IRIS: Set<Array<Int>> = setOf(
        Connection.create(469, 470),
        Connection.create(470, 471),
        Connection.create(471, 472),
        Connection.create(472, 469)
    )
    val FACE_LANDMARKS_FACE_OVAL: Set<Array<Int>> = setOf(
        Connection.create(10, 338),
        Connection.create(338, 297),
        Connection.create(297, 332),
        Connection.create(332, 284),
        Connection.create(284, 251),
        Connection.create(251, 389),
        Connection.create(389, 356),
        Connection.create(356, 454),
        Connection.create(454, 323),
        Connection.create(323, 361),
        Connection.create(361, 288),
        Connection.create(288, 397),
        Connection.create(397, 365),
        Connection.create(365, 379),
        Connection.create(379, 378),
        Connection.create(378, 400),
        Connection.create(400, 377),
        Connection.create(377, 152),
        Connection.create(152, 148),
        Connection.create(148, 176),
        Connection.create(176, 149),
        Connection.create(149, 150),
        Connection.create(150, 136),
        Connection.create(136, 172),
        Connection.create(172, 58),
        Connection.create(58, 132),
        Connection.create(132, 93),
        Connection.create(93, 234),
        Connection.create(234, 127),
        Connection.create(127, 162),
        Connection.create(162, 21),
        Connection.create(21, 54),
        Connection.create(54, 103),
        Connection.create(103, 67),
        Connection.create(67, 109),
        Connection.create(109, 10)
    )
    val FACE_LANDMARKS_CONNECTORS: List<Array<Int>> = listOf(
        FACE_LANDMARKS_LIPS,
        FACE_LANDMARKS_LEFT_EYE,
        FACE_LANDMARKS_LEFT_EYE_BROW,
        FACE_LANDMARKS_RIGHT_EYE,
        FACE_LANDMARKS_RIGHT_EYE_BROW,
        FACE_LANDMARKS_FACE_OVAL
    ).flatten().toList()





    abstract class Connection {
        abstract fun start(): Int
        abstract fun end(): Int

        companion object {
            fun create(start: Int, end: Int): Array<Int> {
                return arrayOf(start, end)
            }
        }
    }
}