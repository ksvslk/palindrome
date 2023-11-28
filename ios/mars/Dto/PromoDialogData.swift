//
//  PromoDialogData.swift
//  mars
//
//  Created by Mina on 28.11.2023.
//

import Foundation

struct PromoDialogData: Codable {
    var activated: Bool?
    var adNumber:Int?
    var levelIds: [Int]?
    var link: String?
    var picturePath: String?
    var title: String?
    var subtitle: String?

    private enum CodingKeys: String, CodingKey {
        case activated
        case adNumber
        case levelIds
        case link
        case picturePath
        case title
        case subtitle
    }
}
