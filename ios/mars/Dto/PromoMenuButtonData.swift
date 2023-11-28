//
//  PromoMenuButtonData.swift
//  mars
//
//  Created by Mina on 28.11.2023.
//

import Foundation

struct PromoMenuButtonData: Codable {
    var activated: Bool?
    var link: String?
    var title: String?

    private enum CodingKeys: String, CodingKey {
        case activated
        case link
        case title
    }
}
