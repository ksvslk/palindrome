//
//  PromoDialogData.swift
//  mars
//
//  Created by Mina on 28.11.2023.
//

import Foundation

struct PromoDialogData: Codable {
    var activated: Bool?
    var levelIds: [Int]?
    var link: String?
    var picturePath: String?
    var text: String?
    var title: String?

    private enum CodingKeys: String, CodingKey {
        case activated
        case levelIds
        case link
        case picturePath
        case text
        case title
    }
}
